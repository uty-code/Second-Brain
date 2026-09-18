package com.aimsgraph.ingest;

import com.aimsgraph.api.NotificationController;
import com.aimsgraph.domain.workspace.WorkspaceCredentialsService;
import com.aimsgraph.domain.workspace.WorkspaceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

  @org.springframework.beans.factory.annotation.Value("${llm.api-key:demo}")
  private String defaultApiKey;

  @org.springframework.beans.factory.annotation.Value(
      "${llm.api-url:https://api.openai.com/v1/chat/completions}")
  private String openAiApiUrl;

  @org.springframework.beans.factory.annotation.Value("${wiki.base.dir:workspaces}")
  private String wikiBaseDir;

  private final WorkspaceService workspaceService;
  private final WorkspaceCredentialsService credentialsService;
  private final Neo4jClient neo4jClient;
  private final NotionIngestService notionIngestService;
  private final com.aimsgraph.ingest.validator.WikiPageValidator wikiPageValidator;
  private final Map<String, ChatModel> modelCache = new ConcurrentHashMap<>();
  private final Map<String, java.util.concurrent.locks.ReentrantLock> fileLocks =
      new ConcurrentHashMap<>();

  private java.util.concurrent.locks.ReentrantLock getFileLock(String filePath) {
    return fileLocks.computeIfAbsent(filePath, k -> new java.util.concurrent.locks.ReentrantLock());
  }

  private String getApiKey(String workspaceId) {
    String key = System.getenv("OPENAI_API_KEY");
    if (key != null && !key.isEmpty()) return key;
    return defaultApiKey;
  }

  private ChatModel getOrCreateModel(String workspaceId, String modelName) {
    String cacheKey = workspaceId + ":" + (modelName != null ? modelName : "gpt-4o-mini");
    return modelCache.computeIfAbsent(
        cacheKey,
        k -> {
          if ("deepseek-v4".equalsIgnoreCase(modelName)) {
            log.info(
                "Initializing OpenAiChatModel (OpenRouter - DeepSeek) for workspace: {}",
                workspaceId);
            String envDeepSeekKey = System.getenv("DEEPSEEK_API_KEY");
            com.aimsgraph.domain.workspace.WorkspaceCredentials creds =
                credentialsService.getCredentials(workspaceId);
            String deepseekKey = creds != null ? creds.getDeepseekApiKey() : null;
            String finalKey =
                (deepseekKey != null && !deepseekKey.isBlank()) ? deepseekKey : envDeepSeekKey;
            if (finalKey == null || finalKey.isBlank()) {
              log.warn(
                  "DeepSeek API Key is missing. Fallback to OpenAI API Key (might fail if not supported).");
              finalKey = getApiKey(workspaceId);
            }
            return OpenAiChatModel.builder()
                .apiKey(finalKey)
                .baseUrl("https://openrouter.ai/api/v1")
                .modelName("deepseek/deepseek-v4-pro")
                .temperature(0.2)
                .maxTokens(4000)
                .build();
          } else {
            String apiKey = getApiKey(workspaceId);
            log.info("Initializing OpenAiChatModel (gpt-4o-mini) for workspace: {}", workspaceId);
            return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini")
                .temperature(0.2)
                .build();
          }
        });
  }

  public String queryDirect(String workspaceId, String userQuery, String modelName) {
    try {
      ChatModel model = getOrCreateModel(workspaceId, modelName);
      return model.chat(userQuery);
    } catch (Exception e) {
      log.error("Direct query failed", e);
      return "Error: " + e.getMessage();
    }
  }

  // ---------------------------------------------------------
  // TEXT INGESTION PIPELINE (IngestionWorker)
  // ---------------------------------------------------------
  public List<ExtractedConcept> extractKnowledge(
      String eventId, String content, String workspaceId) {
    log.info("Extracting knowledge graph for text in workspace: {}", workspaceId);

    ChatModel model = getOrCreateModel(workspaceId, "gpt-4o-mini");

    ConceptExtractor extractor =
        dev.langchain4j.service.AiServices.builder(ConceptExtractor.class).chatModel(model).build();

    ExtractedConceptsResponse response = extractor.extract(content);
    return response != null && response.concepts() != null
        ? response.concepts()
        : java.util.Collections.emptyList();
  }

  // ---------------------------------------------------------
  // FILE INGESTION PIPELINE (AnalyzeController)
  // ---------------------------------------------------------
  public Map<String, Object> analyzeFilesWithOpenAI(
      org.springframework.web.multipart.MultipartFile[] files, String workspaceId, String modelName)
      throws Exception {
    if (getApiKey(workspaceId) == null
        || getApiKey(workspaceId).isEmpty()
        || getApiKey(workspaceId).equals("demo")) {
      throw new RuntimeException("OpenAI API Key is not configured.");
    }

    StringBuilder rawSourceBuilder = new StringBuilder();
    for (org.springframework.web.multipart.MultipartFile file : files) {
      if (!file.isEmpty()) {
        rawSourceBuilder.append("--- File: ").append(file.getOriginalFilename()).append(" ---\n");
        rawSourceBuilder
            .append(new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8))
            .append("\n\n");
      }
    }

    if (rawSourceBuilder.length() == 0) {
      throw new RuntimeException("업로드된 파일이 모두 비어있습니다.");
    }

    String rawSourceText = rawSourceBuilder.toString();
    log.info(
        "Files parsed into text ({} bytes). Routing to analyzeTextWithOpenAI...",
        rawSourceText.length());

    // Delegate to analyzeTextWithOpenAI using the extracted text
    return analyzeTextWithOpenAI(rawSourceText, workspaceId, modelName);
  }

  public Map<String, Object> analyzeTextWithOpenAI(
      String rawSourceText, String workspaceId, String modelName) throws Exception {
    if (getApiKey(workspaceId) == null
        || getApiKey(workspaceId).isEmpty()
        || getApiKey(workspaceId).equals("demo")) {
      throw new RuntimeException("OpenAI API Key is not configured.");
    }

    // === 1단계: 파일 대신 텍스트로 분석하여 그래프(Nodes, Links) 추출 ===
    String analysisResult = callResponsesAPI(null, rawSourceText, workspaceId, modelName);
    log.info("Analysis complete for raw text. Parsing graph data...");

    String cleaned =
        analysisResult
            .replaceAll("^```json\\s*", "")
            .replaceAll("^```\\s*", "")
            .replaceAll("\\s*```$", "");
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> graphData =
        mapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});

    // === 2단계: 동기식 & 병렬 위키 생성 ===
    generateWikiPages(graphData, rawSourceText, workspaceId, modelName);

    // Neo4j에 그래프(노드와 링크) 저장
    saveGraphToNeo4j(graphData, workspaceId);

    // 위키 본문 크로스 레퍼런스([[slug]]) 링크도 Neo4j 그래프에 실시간 동기화
    syncWikiLinksToNeo4j(workspaceId);

    return graphData;
  }

  private String uploadFileToOpenAI(
      org.springframework.web.multipart.MultipartFile file, String workspaceId) throws Exception {
    String boundary = "----FormBoundary" + java.util.UUID.randomUUID().toString().replace("-", "");
    byte[] fileBytes = file.getBytes();
    String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";

    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    baos.write(("--" + boundary + "\r\n").getBytes());
    baos.write("Content-Disposition: form-data; name=\"purpose\"\r\n\r\n".getBytes());
    baos.write("user_data\r\n".getBytes());
    baos.write(("--" + boundary + "\r\n").getBytes());
    baos.write(
        ("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n")
            .getBytes());
    baos.write(
        ("Content-Type: "
                + (file.getContentType() != null
                    ? file.getContentType()
                    : "application/octet-stream")
                + "\r\n\r\n")
            .getBytes());
    baos.write(fileBytes);
    baos.write(("\r\n--" + boundary + "--\r\n").getBytes());
    byte[] bodyBytes = baos.toByteArray();

    java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
    java.net.http.HttpRequest request =
        java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("https://api.openai.com/v1/files"))
            .header("Authorization", "Bearer " + getApiKey(workspaceId))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
            .build();

    java.net.http.HttpResponse<String> resp =
        client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> respMap =
        mapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});

    if (respMap.get("error") != null) {
      throw new RuntimeException("OpenAI file upload failed: " + respMap.get("error"));
    }
    return (String) respMap.get("id");
  }

  private String callResponsesAPI(
      java.util.List<String> fileIds, String rawSourceText, String workspaceId, String modelName)
      throws Exception {
    java.util.Collection<Map<String, Object>> existingNodes =
        neo4jClient
            .query("MATCH (n:Concept {workspaceId: $workspaceId}) RETURN n.name as name LIMIT 200")
            .bind(workspaceId)
            .to("workspaceId")
            .fetch()
            .all();
    List<String> existingNames = new java.util.ArrayList<>();
    for (Map<String, Object> record : existingNodes) {
      if (record.get("name") != null) existingNames.add((String) record.get("name"));
    }
    String existingConceptsStr =
        existingNames.isEmpty() ? "None" : String.join(", ", existingNames);

    String userPrompt =
        "Here are the existing concepts already in the knowledge graph: ["
            + existingConceptsStr
            + "]\n"
            + "CRITICAL RULE: If a concept in the document strongly matches or relates to an existing concept, you MUST reuse the exact same ID. Only generate new IDs for entirely new concepts.\n\n"
            + "Analyze the attached document(s) and build a single UNIFIED knowledge graph.\n\n"
            + "Raw Document Content:\n"
            + rawSourceText;

    ChatModel model = getOrCreateModel(workspaceId, modelName != null ? modelName : "gpt-4o-mini");

    log.info(
        "Calling Language Model for unified knowledge graph extraction using Structured Outputs...");
    UnifiedGraphExtractor extractor =
        dev.langchain4j.service.AiServices.builder(UnifiedGraphExtractor.class)
            .chatModel(model)
            .build();

    KnowledgeGraphResponse graphResponse = extractor.extract(userPrompt);

    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(graphResponse);
  }

  // ---------------------------------------------------------
  // PARALLEL WIKI GENERATION
  // ---------------------------------------------------------
  public void generateWikiPages(
      Map<String, Object> graphData, String rawSourceText, String workspaceId, String modelName) {
    java.util.List<Map<String, Object>> nodes =
        (java.util.List<Map<String, Object>>) graphData.get("nodes");
    if (nodes == null || nodes.isEmpty()) return;

    java.util.Map<String, java.util.List<String>> edgeMap = new java.util.HashMap<>();
    java.util.List<Map<String, Object>> links =
        (java.util.List<Map<String, Object>>) graphData.get("links");
    if (links != null) {
      for (Map<String, Object> link : links) {
        String source = (String) link.get("source");
        String target = (String) link.get("target");
        if (source != null && target != null) {
          edgeMap.computeIfAbsent(source, k -> new java.util.ArrayList<>()).add(target);
          edgeMap.computeIfAbsent(target, k -> new java.util.ArrayList<>()).add(source);
        }
      }
    }

    log.info("Starting synchronous parallel wiki generation for {} nodes...", nodes.size());
    try {
      java.nio.file.Path wikiDir =
          java.nio.file.Paths.get(wikiBaseDir, workspaceId, "wiki", "concepts");
      java.nio.file.Files.createDirectories(wikiDir);

      ObjectMapper mapper = new ObjectMapper();

      String systemPrompt =
          "You are a specialized agent for writing structured Markdown Wiki pages for a Zettelkasten-style Second Brain system.\n\n"
              + "## Output Rules\n"
              + "- ALL content (title, summary, body) MUST be written in Korean.\n"
              + "- Cross-references to other concepts MUST use `[[english-slug-id]]` syntax (e.g. `[[event-driven-architecture]]`). NEVER use Korean text inside `[[...]]`.\n"
              + "- The `id` field MUST be an english slug with hyphens (e.g. `event-driven-architecture`).\n\n"
              + "## Document Structure & Quality Guidelines\n"
              + "- The section headings (## headers) and internal structure MUST be tailored dynamically to the nature of the concept (software, mathematics, science, history, humanities, product, biography, etc.).\n"
              + "- Structure the content logically with at least 2 relevant ## headings suited to the topic. Do NOT force a rigid uniform template on all concepts.\n"
              + "- Every section must contain meaningful, well-explained paragraphs or structured lists/tables.\n\n"
              + "## KNOWLEDGE BOUNDARY & HALLUCINATION PREVENTION\n"
              + "- Separate information clearly:\n"
              + "  A. Information directly supported by the source text.\n"
              + "  B. General domain knowledge used to explain or clarify the concept.\n"
              + "- When the source text is concise, you MAY and SHOULD supplement with widely-known general knowledge (e.g. standard patterns, foundational theories, historical context).\n"
              + "- When using general knowledge, keep it technically accurate and DO NOT imply that it was stated in the user's source text.\n"
              + "- You MUST NEVER fabricate project-specific details as facts unless explicitly in the source text:\n"
              + "  * NEVER invent fictional project architecture or system designs.\n"
              + "  * NEVER fabricate implementation details, performance metrics, benchmarks, or test results.\n"
              + "  * NEVER generate fake code claiming to represent the user's project codebase.\n"
              + "  * Do NOT introduce unrelated technologies. When mentioning additional concepts as general knowledge, they must be directly relevant to explaining the concept.\n\n"
              + "## CROSS-REFERENCE RULES\n"
              + "- ONLY reference concept IDs explicitly provided in the AVAILABLE CONCEPT IDS list.\n"
              + "- ACTIVELY link to other concepts from the AVAILABLE CONCEPT IDS list in the body text using `[[english-slug]]` syntax whenever relevant. Strive to link to at least 1-2 related concepts.\n"
              + "- NEVER invent a new wiki ID in a cross-reference.\n"
              + "- Do NOT create self-references to the current concept (never link to the page itself).\n\n"
              + "## Format & Quality\n"
              + "- Use markdown elements such as tables, bullet points, code blocks, math expressions, and blockquotes where appropriate to structure the document clearly.\n"
              + "- summary 필드는 30~80자 사이의 한 문장으로 압축.\n\n"
              + "## Examples of Flexible Structures Across Domains:\n"
              + "```json\n"
              + "{\"id\":\"event-driven-architecture\",\"title\":\"이벤트 드리븐 아키텍처\",\"type\":\"concept\","
              + "\"summary\":\"시스템 컴포넌트 간 비동기 이벤트를 통해 느슨한 결합을 달성하는 소프트웨어 아키텍처 패턴\","
              + "\"tags\":[\"아키텍처\",\"비동기\"],\"aliases\":[\"EDA\"],"
              + "\"content\":\"## 개요\\n\\n**이벤트 드리븐 아키텍처(EDA)**는 상태 변화를 비동기 이벤트로 발행하고 감지하여 처리하는 분산 소프트웨어 설계 패턴입니다.\\n\\n"
              + "## 핵심 컴포넌트\\n\\n| 컴포넌트 | 역할 |\\n|---|---|\\n| Event Producer | 상태 변경 이벤트를 생성하여 메시지 브로커에 전송 |\\n| Event Consumer | 토픽을 구독하여 독립 비즈니스 로직 수행 |\\n\\n"
              + "## 아키텍처 트레이드오프\\n\\n시스템 간 결합도를 낮추고 수평 확장이 용이하지만, 최종 일관성 보장과 분산 트레이싱을 위한 추가 설계가 필요합니다.\","
              + "\"relatedConcepts\":[\"message-broker\"]}\n"
              + "```\n";

      Map<String, Object> responseFormat =
          Map.of(
              "type",
              "json_schema",
              "json_schema",
              Map.of(
                  "name",
                  "wiki_page",
                  "strict",
                  true,
                  "schema",
                  Map.of(
                      "type",
                      "object",
                      "properties",
                      Map.of(
                          "id", Map.of("type", "string"),
                          "title", Map.of("type", "string"),
                          "type", Map.of("type", "string"),
                          "summary", Map.of("type", "string"),
                          "tags", Map.of("type", "array", "items", Map.of("type", "string")),
                          "aliases", Map.of("type", "array", "items", Map.of("type", "string")),
                          "content", Map.of("type", "string"),
                          "relatedConcepts",
                              Map.of("type", "array", "items", Map.of("type", "string"))),
                      "required",
                      List.of(
                          "id",
                          "title",
                          "type",
                          "summary",
                          "tags",
                          "aliases",
                          "content",
                          "relatedConcepts"),
                      "additionalProperties",
                      false)));

      // API Rate Limiting 보호를 위해 동시 실행 10개로 제한
      java.util.concurrent.Semaphore rateLimiter = new java.util.concurrent.Semaphore(10);
      java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

      java.util.List<String> allConceptIds =
          nodes.stream().map(n -> (String) n.get("id")).filter(java.util.Objects::nonNull).toList();

      try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();

        for (Map<String, Object> node : nodes) {
          futures.add(
              executor.submit(
                  () -> {
                    try {
                      rateLimiter.acquire();
                      try {
                        String currentNodeId = (String) node.get("id");
                        java.util.List<String> otherConceptIds =
                            allConceptIds.stream()
                                .filter(
                                    id ->
                                        currentNodeId == null
                                            || !currentNodeId.equalsIgnoreCase(id))
                                .toList();
                        String availableIdsStr =
                            otherConceptIds.isEmpty()
                                ? "(None)"
                                : String.join("\n- ", otherConceptIds);

                        String nodeJson = mapper.writeValueAsString(node);
                        String snippet = (String) node.get("snippet");
                        String userPrompt =
                            "Here is the ORIGINAL SOURCE TEXT provided by the user:\n"
                                + "==============================\n"
                                + rawSourceText
                                + "\n"
                                + "==============================\n\n"
                                + "And here is the EXACT QUOTE (Snippet) where this specific concept is mentioned:\n"
                                + ">>> "
                                + snippet
                                + " <<<\n\n"
                                + "AVAILABLE CONCEPT IDS FOR CROSS-REFERENCES (Excludes self-reference):\n- "
                                + availableIdsStr
                                + "\n\n"
                                + "Based on the ENTIRE source text for deep context, but FOCUSING HEAVILY on the exact quote above, generate a detailed, high-quality wiki page for THIS SPECIFIC concept:\n"
                                + nodeJson
                                + "\n\n"
                                + "Maximize depth, accuracy, and capture the author's original intent. Adhere strictly to the KNOWLEDGE BOUNDARY and CROSS-REFERENCE RULES (NEVER link to this concept itself).";

                        java.util.List<Map<String, Object>> messages = new java.util.ArrayList<>();
                        messages.add(Map.of("role", "system", "content", systemPrompt));
                        messages.add(Map.of("role", "user", "content", userPrompt));

                        String requestModelName = "gpt-4o-mini";
                        String apiUrl =
                            (openAiApiUrl != null && !openAiApiUrl.isBlank())
                                ? openAiApiUrl
                                : "https://api.openai.com/v1/chat/completions";
                        String apiKey = getApiKey(workspaceId);

                        if ("deepseek-v4".equalsIgnoreCase(modelName)) {
                          requestModelName = "deepseek/deepseek-v4-pro";
                          apiUrl = "https://openrouter.ai/api/v1/chat/completions";

                          String envDeepSeekKey = System.getenv("DEEPSEEK_API_KEY");
                          com.aimsgraph.domain.workspace.WorkspaceCredentials creds =
                              credentialsService.getCredentials(workspaceId);
                          String deepseekKey = creds != null ? creds.getDeepseekApiKey() : null;
                          String finalKey =
                              (deepseekKey != null && !deepseekKey.isBlank())
                                  ? deepseekKey
                                  : envDeepSeekKey;
                          if (finalKey != null && !finalKey.isBlank()) {
                            apiKey = finalKey;
                          }
                        }

                        // === LLM OUTPUT VALIDATION & RETRY WITH FEEDBACK (최대 3회: 최초 1회 + Retry 2회)
                        // ===
                        StructuredWikiPage validPage = null;
                        int maxAttempts = 3;
                        java.util.Set<String> availableIdSet =
                            new java.util.HashSet<>(allConceptIds);

                        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                          java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
                          requestBody.put("model", requestModelName);
                          requestBody.put("messages", new java.util.ArrayList<>(messages));
                          requestBody.put("response_format", responseFormat);
                          if ("deepseek-v4".equalsIgnoreCase(modelName)) {
                            requestBody.put("max_tokens", 4000);
                          }

                          String jsonBody = mapper.writeValueAsString(requestBody);
                          java.net.http.HttpRequest request =
                              java.net.http.HttpRequest.newBuilder()
                                  .uri(java.net.URI.create(apiUrl))
                                  .timeout(java.time.Duration.ofSeconds(45))
                                  .header("Content-Type", "application/json")
                                  .header("Authorization", "Bearer " + apiKey)
                                  .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                                  .build();

                          java.net.http.HttpResponse<String> resp =
                              client.send(
                                  request, java.net.http.HttpResponse.BodyHandlers.ofString());
                          Map<String, Object> respMap =
                              mapper.readValue(
                                  resp.body(),
                                  new com.fasterxml.jackson.core.type.TypeReference<
                                      Map<String, Object>>() {});

                          if (respMap.get("error") != null) {
                            log.error(
                                "Wiki generation failed for node {} (attempt {}): {}",
                                node.get("name"),
                                attempt,
                                respMap.get("error"));
                            break;
                          }

                          String outputText = "";
                          java.util.List<Map<String, Object>> choices =
                              (java.util.List<Map<String, Object>>) respMap.get("choices");
                          if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> message =
                                (Map<String, Object>) choices.get(0).get("message");
                            if (message != null) {
                              outputText = (String) message.get("content");
                            }
                          }

                          String cleaned =
                              outputText
                                  .trim()
                                  .replaceAll("^```json\\s*", "")
                                  .replaceAll("^```\\s*", "")
                                  .replaceAll("\\s*```$", "");

                          StructuredWikiPage candidatePage = null;
                          try {
                            candidatePage = mapper.readValue(cleaned, StructuredWikiPage.class);
                          } catch (Exception parseE) {
                            log.warn(
                                "Attempt {}/{}: JSON parsing failed for node {}: {}",
                                attempt,
                                maxAttempts,
                                node.get("name"),
                                parseE.getMessage());
                          }

                          // 순수 검증기 호출 (LLM/DB 접근 없음)
                          com.aimsgraph.ingest.validator.ValidationResult valResult =
                              wikiPageValidator.validate(candidatePage, availableIdSet);

                          if (valResult.valid()) {
                            validPage = valResult.sanitizedPage();
                            if (!valResult.errors().isEmpty()) {
                              log.info(
                                  "Node {} auto-sanitized: {}",
                                  node.get("name"),
                                  valResult.errors());
                            }
                            break; // 검증 통과!
                          }

                          log.warn(
                              "Attempt {}/{} failed validation for node {}: {}",
                              attempt,
                              maxAttempts,
                              node.get("name"),
                              valResult.errors());

                          if (attempt < maxAttempts) {
                            String feedback =
                                valResult.buildFeedbackPrompt(
                                    candidatePage != null
                                        ? candidatePage.title()
                                        : (String) node.get("name"),
                                    candidatePage != null
                                        ? candidatePage.id()
                                        : (String) node.get("id"));
                            messages.add(Map.of("role", "assistant", "content", outputText));
                            messages.add(Map.of("role", "user", "content", feedback));
                          } else {
                            log.error(
                                "Node {} failed validation after {} attempts. REJECTING wiki creation to prevent data corruption. Errors: {}",
                                node.get("name"),
                                maxAttempts,
                                valResult.errors());
                          }
                        }

                        // 3회 모두 실패 시 persistence 금지 (오염 방지)
                        if (validPage == null) {
                          log.warn("Skipping persistence for rejected node: {}", node.get("name"));
                          return;
                        }

                        StructuredWikiPage page = validPage;
                        if (page.id() != null && !page.id().isBlank()) {
                          java.util.List<String> actualLinks =
                              edgeMap.getOrDefault(page.id(), new java.util.ArrayList<>());
                          java.util.List<String> validRelatedConcepts =
                              actualLinks.stream().distinct().toList();

                          page =
                              new StructuredWikiPage(
                                  page.id(),
                                  page.title(),
                                  page.type(),
                                  page.summary(),
                                  page.tags(),
                                  page.aliases(),
                                  page.content(),
                                  validRelatedConcepts);

                          StringBuilder sb = new StringBuilder();
                          sb.append("---\n");
                          sb.append("title: ")
                              .append(page.title() != null ? page.title() : "")
                              .append("\n");
                          sb.append("type: ")
                              .append(page.type() != null ? page.type() : "")
                              .append("\n");
                          sb.append("created_at: ")
                              .append(java.time.LocalDate.now().toString())
                              .append("\n");
                          sb.append("source_supported: true\n");
                          sb.append("general_knowledge_supplemented: true\n");
                          if (page.tags() != null && !page.tags().isEmpty()) {
                            sb.append("tags:\n");
                            for (String tag : page.tags()) {
                              sb.append("  - ").append(tag).append("\n");
                            }
                          }
                          if (page.aliases() != null && !page.aliases().isEmpty()) {
                            sb.append("aliases:\n");
                            for (String alias : page.aliases()) {
                              sb.append("  - ").append(alias).append("\n");
                            }
                          }
                          sb.append("---\n\n");
                          sb.append(page.content() != null ? page.content() : "").append("\n\n");

                          if (!validRelatedConcepts.isEmpty()) {
                            sb.append("## 관련 개념들\n");
                            for (String related : validRelatedConcepts) {
                              sb.append("- [[").append(related).append("]]\n");
                            }
                          }

                          String filename = page.id().trim() + ".md";
                          java.nio.file.Path filePath = wikiDir.resolve(filename);

                          java.util.concurrent.locks.ReentrantLock lock =
                              getFileLock(filePath.toAbsolutePath().toString());
                          lock.lock();
                          try {
                            if (java.nio.file.Files.exists(filePath)) {
                              String existingContent =
                                  java.nio.file.Files.readString(
                                      filePath, java.nio.charset.StandardCharsets.UTF_8);
                              java.nio.file.attribute.FileTime lastModifiedTime =
                                  java.nio.file.Files.getLastModifiedTime(filePath);
                              String existingDate =
                                  lastModifiedTime
                                      .toInstant()
                                      .atZone(java.time.ZoneId.systemDefault())
                                      .toLocalDate()
                                      .toString();
                              String newDate = java.time.LocalDate.now().toString();

                              String mergedContent =
                                  mergeWikiContent(
                                      existingContent,
                                      sb.toString(),
                                      page.title(),
                                      workspaceId,
                                      modelName,
                                      existingDate,
                                      newDate);
                              java.nio.file.Files.writeString(
                                  filePath, mergedContent, java.nio.charset.StandardCharsets.UTF_8);
                              log.info("Merged and refined existing wiki page: {}", filename);
                            } else {
                              java.nio.file.Files.writeString(
                                  filePath, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
                              log.info("Saved wiki page: {}", filename);
                            }
                          } finally {
                            lock.unlock();
                          }
                        }
                      } finally {
                        rateLimiter.release();
                      }
                    } catch (Exception innerE) {
                      log.error("Error generating wiki for individual node", innerE);
                    }
                  }));
        }

        // Wait for all virtual thread tasks to complete
        for (var future : futures) {
          try {
            future.get();
          } catch (Exception e) {
            log.error("Error waiting for wiki generation task", e);
          }
        }
      }

      log.info(
          "Wiki generation completed successfully! Files saved to {}", wikiDir.toAbsolutePath());

      // === LLM Wiki Pattern: index.md & log.md 자동 갱신 ===
      updateIndexAndLog(wikiDir, workspaceId, rawSourceText);

    } catch (Exception e) {
      log.error("Error in generateWikiPages", e);
    }
  }

  // ---------------------------------------------------------
  // LLM WIKI PATTERN: Intelligent Merge & Refine Engine
  // ---------------------------------------------------------
  public String mergeWikiContent(
      String existingContent,
      String newContent,
      String title,
      String workspaceId,
      String modelName,
      String existingDate,
      String newDate) {
    int maxAttempts = 3; // 총 3 calls (최초 1회 + 재시도 2회)
    String currentPrompt =
        "You are an expert wiki editor for a Zettelkasten Second Brain system.\n\n"
            + "Below are TWO versions of a wiki page about \""
            + title
            + "\".\n"
            + "Your task is to MERGE them into ONE cohesive, consolidated wiki document.\n\n"
            + "MERGE & SYNTHESIS RULES:\n"
            + "1. Preserve all unique, factual, and useful information from both versions.\n"
            + "2. Remove redundant explanations and duplicated facts. Prefer concise synthesis over repetitive preservation.\n"
            + "3. Do not simply concatenate the two documents. Reorganize content to flow logically as a single unified page.\n"
            + "4. Do NOT increase document length merely by appending information. Consolidate overlapping concepts.\n"
            + "5. CONTRADICTION & TEMPORAL CHANGE HANDLING:\n"
            + "   - Compare the version dates (Existing: "
            + existingDate
            + ", New: "
            + newDate
            + ").\n"
            + "   - Determine whether differences represent a temporal change/evolution, contextual variation, or factual contradiction.\n"
            + "   - If the difference is a clear temporal evolution (e.g. newer version updates an architecture, tool version, or project status), reflect the updated state as the current reality while explicitly preserving what specific legacy version or technology was replaced (e.g. explicitly state both the previous version like 'Java 17' and previous architecture like '스레드 풀') in the background/history.\n"
            + "   - If the contradiction represents divergent architectural perspectives (e.g. strong consistency vs high availability tradeoffs), explicitly preserve both viewpoints without arbitrarily choosing one.\n"
            + "   - Never silently overwrite an existing factual claim without evaluating temporal and contextual validity.\n"
            + "6. KNOWLEDGE INTEGRITY:\n"
            + "   - Do not introduce new unverified project-specific facts.\n"
            + "   - Do not invent new cross-reference IDs. Only keep valid `[[english-slug]]` links.\n"
            + "7. DOCUMENT STRUCTURE:\n"
            + "   - YAML frontmatter (preserve and merge tags, aliases, provenance fields)\n"
            + "   - Preserve the document's own section headings and natural flow, consolidating duplicate sections\n"
            + "   - Adapt the section structure naturally if new information introduces new aspects\n"
            + "   - ## 관련 개념들 (keep merged `[[english-slug]]` links)\n"
            + "8. Output ONLY the final merged markdown. No explanations or commentary.\n"
            + "9. ALL text must be in Korean. Cross-references use `[[english-slug]]` format.\n\n"
            + "=== EXISTING VERSION (Last Modified: "
            + existingDate
            + ") ===\n"
            + existingContent
            + "\n\n"
            + "=== NEW VERSION (Generated: "
            + newDate
            + ") ===\n"
            + newContent;

    ChatModel model = getOrCreateModel(workspaceId, modelName != null ? modelName : "gpt-4o-mini");
    String feedback = "";

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        String promptToSend = currentPrompt;
        if (!feedback.isBlank()) {
          promptToSend +=
              "\n\n[PREVIOUS ATTEMPT VALIDATION FAILURE - PLEASE FIX THE FOLLOWING ISSUES]:\n"
                  + feedback;
        }

        String merged = model.chat(promptToSend);
        if (merged != null && !merged.isBlank()) {
          String cleaned =
              merged
                  .trim()
                  .replaceAll("^```markdown\\s*", "")
                  .replaceAll("^```\\s*", "")
                  .replaceAll("\\s*```$", "");

          if (wikiPageValidator != null) {
            String fallbackId =
                title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
            if (fallbackId.isBlank()) fallbackId = "wiki-concept";
            var valResult =
                wikiPageValidator.validateMarkdownDocument(cleaned, fallbackId, title, null);
            if (valResult.valid()) {
              log.info(
                  "Merge succeeded and passed validation on attempt {}/{} for '{}'",
                  attempt,
                  maxAttempts,
                  title);
              return cleaned;
            } else {
              log.warn(
                  "Merge attempt {}/{} failed validation for '{}': {}",
                  attempt,
                  maxAttempts,
                  title,
                  valResult.errors());
              if (attempt < maxAttempts) {
                feedback = valResult.buildFeedbackPrompt(title, fallbackId);
                continue;
              }
            }
          } else {
            return cleaned;
          }
        }
      } catch (Exception e) {
        log.warn(
            "Merge call attempt {}/{} failed for '{}': {}",
            attempt,
            maxAttempts,
            title,
            e.getMessage());
      }
    }

    // 3회 시도 모두 실패 시 안전 Fallback: 기존 문서를 온전히 보존하고 Append/신규 덮어쓰기 차단
    log.error(
        "Merge for '{}' failed validation after {} attempts. FALLBACK: Preserving existing wiki"
            + " content to prevent corruption.",
        title,
        maxAttempts);
    return existingContent;
  }

  // ---------------------------------------------------------
  // LLM WIKI PATTERN: index.md & log.md Auto-Update
  // ---------------------------------------------------------
  private void updateIndexAndLog(
      java.nio.file.Path wikiDir, String workspaceId, String rawSourceText) {
    try {
      java.nio.file.Path wikiRoot = wikiDir.getParent(); // wiki/ folder
      if (wikiRoot == null) wikiRoot = wikiDir;

      // === index.md: 전체 위키 카탈로그 재빌드 ===
      java.nio.file.Path indexPath = wikiRoot.resolve("index.md");
      StringBuilder indexBuilder = new StringBuilder();
      indexBuilder.append("# Wiki Index\n\n");
      indexBuilder.append("> 이 파일은 위키의 전체 개념 카탈로그입니다. 자동으로 갱신됩니다.\n\n");
      indexBuilder.append("| 개념 | 타입 | 요약 |\n");
      indexBuilder.append("|------|------|------|\n");

      if (java.nio.file.Files.exists(wikiDir)) {
        java.util.List<java.nio.file.Path> sortedFiles =
            java.nio.file.Files.list(wikiDir)
                .filter(p -> p.toString().endsWith(".md"))
                .sorted()
                .toList();

        for (java.nio.file.Path file : sortedFiles) {
          String content =
              java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
          String fileName = file.getFileName().toString();
          String slug = fileName.replace(".md", "");

          // Parse frontmatter
          String pageTitle = slug;
          String pageType = "-";
          java.util.regex.Matcher titleMatcher =
              java.util.regex.Pattern.compile("title:\\s*(.*)").matcher(content);
          if (titleMatcher.find()) pageTitle = titleMatcher.group(1).trim();
          java.util.regex.Matcher typeMatcher =
              java.util.regex.Pattern.compile("type:\\s*(.*)").matcher(content);
          if (typeMatcher.find()) pageType = typeMatcher.group(1).trim();

          // Extract summary from frontmatter or first meaningful sentence
          String summary = "-";
          java.util.regex.Matcher sumMatcher =
              java.util.regex.Pattern.compile("summary:\\s*(.*)").matcher(content);
          if (sumMatcher.find() && !sumMatcher.group(1).trim().isBlank()) {
            summary = sumMatcher.group(1).trim();
          } else {
            java.util.regex.Matcher firstHeaderMatcher =
                java.util.regex.Pattern.compile("## [^\\n]+\\s*\\n+\\s*(.+)").matcher(content);
            if (firstHeaderMatcher.find()) {
              summary = firstHeaderMatcher.group(1).trim();
            }
          }
          if (summary.length() > 80) summary = summary.substring(0, 77) + "...";

          indexBuilder
              .append("| [[")
              .append(slug)
              .append("\\|")
              .append(pageTitle)
              .append("]] | ")
              .append(pageType)
              .append(" | ")
              .append(summary)
              .append(" |\n");
        }
      }

      java.nio.file.Files.writeString(
          indexPath, indexBuilder.toString(), java.nio.charset.StandardCharsets.UTF_8);
      log.info("Updated wiki index.md with {} entries", java.nio.file.Files.list(wikiDir).count());

      // === log.md: Chronological ingest log ===
      java.nio.file.Path logPath = wikiRoot.resolve("log.md");
      StringBuilder logEntry = new StringBuilder();
      if (!java.nio.file.Files.exists(logPath)) {
        logEntry.append("# Wiki Log\n\n> 자동 생성된 수집 히스토리 타임라인입니다.\n\n");
      }

      String sourcePreview =
          rawSourceText.length() > 100
              ? rawSourceText.substring(0, 100).replace("\n", " ") + "..."
              : rawSourceText.replace("\n", " ");
      logEntry
          .append("## [")
          .append(java.time.LocalDate.now())
          .append("] ingest | ")
          .append(workspaceId)
          .append("\n");
      logEntry.append("- **시각**: ").append(java.time.LocalDateTime.now().toString()).append("\n");
      logEntry.append("- **소스 미리보기**: ").append(sourcePreview).append("\n\n");

      java.nio.file.Files.writeString(
          logPath,
          logEntry.toString(),
          java.nio.charset.StandardCharsets.UTF_8,
          java.nio.file.StandardOpenOption.CREATE,
          java.nio.file.StandardOpenOption.APPEND);
      log.info("Appended ingest entry to wiki log.md");

    } catch (Exception e) {
      log.warn("Failed to update index/log (non-critical): {}", e.getMessage());
    }
  }

  void saveGraphToNeo4j(Map<String, Object> graphData, String workspaceId) {
    if (graphData == null) return;

    java.util.Set<String> validNodeIds = new java.util.HashSet<>();
    List<Map<String, Object>> nodes = (List<Map<String, Object>>) graphData.get("nodes");
    if (nodes != null) {
      for (Map<String, Object> node : nodes) {
        String id = (String) node.get("id");
        if (id == null || id.isBlank()) continue;
        String normalizedId = id.trim().toLowerCase();
        validNodeIds.add(normalizedId);
        String title = (String) node.get("name");
        if (title == null) title = normalizedId;

        String createNodeCypher =
            "MERGE (c:Concept {name: $name, workspaceId: $workspaceId}) "
                + "ON CREATE SET c.title = $title, c.createdAt = datetime() "
                + "ON MATCH SET c.title = $title, c.updatedAt = datetime()";
        neo4jClient
            .query(createNodeCypher)
            .bind(normalizedId)
            .to("name")
            .bind(workspaceId)
            .to("workspaceId")
            .bind(title)
            .to("title")
            .run();
      }
    }

    java.util.regex.Pattern slugPattern =
        java.util.regex.Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    List<Map<String, Object>> links = (List<Map<String, Object>>) graphData.get("links");
    if (links != null) {
      java.util.Set<String> processedEdges = new java.util.HashSet<>();
      for (Map<String, Object> link : links) {
        String source = (String) link.get("source");
        String target = (String) link.get("target");
        if (source == null || source.isBlank() || target == null || target.isBlank()) continue;

        String normSource = source.trim().toLowerCase();
        String normTarget = target.trim().toLowerCase();

        // 1. 자기참조 차단
        if (normSource.equalsIgnoreCase(normTarget)) {
          log.warn(
              "Self-reference LLM edge rejected in workspace {}: source and target are identical ('{}'). Edge skipped.",
              workspaceId,
              normSource);
          continue;
        }

        // 2. Slug 포맷 검증
        if (!slugPattern.matcher(normSource).matches()
            || !slugPattern.matcher(normTarget).matches()) {
          log.warn(
              "Invalid slug format in LLM edge in workspace {}: source='{}', target='{}'. Edge skipped.",
              workspaceId,
              normSource,
              normTarget);
          continue;
        }

        // 3. Phantom Node 방어 (nodes에 실존하지 않는 노드 연결 차단)
        if (!validNodeIds.contains(normSource) || !validNodeIds.contains(normTarget)) {
          log.warn(
              "Phantom node edge rejected in workspace {}: source '{}' or target '{}' does not exist in workspace nodes. Edge skipped.",
              workspaceId,
              normSource,
              normTarget);
          continue;
        }

        // 4. 중복 엣지 방지 (인메모리 de-duplication)
        String edgeKey = normSource + "->" + normTarget;
        if (!processedEdges.add(edgeKey)) {
          continue;
        }

        String relLabel = (String) link.get("label");
        if (relLabel == null || relLabel.isBlank()) {
          relLabel = "RELATES_TO";
        } else {
          relLabel = relLabel.toUpperCase().replaceAll("[^A-Z_]", "");
          if (relLabel.isEmpty()) relLabel = "RELATES_TO";
        }

        String linkCypher =
            "MERGE (c:Concept {name: $source, workspaceId: $workspaceId}) "
                + "MERGE (l:Concept {name: $target, workspaceId: $workspaceId}) "
                + "MERGE (c)-[r:REFERENCES]->(l) "
                + "ON CREATE SET r.relationSource = 'LLM_INFERRED', "
                + "              r.sources = ['LLM_INFERRED'], "
                + "              r.label = $label, "
                + "              r.createdAt = datetime(), "
                + "              r.updatedAt = datetime() "
                + "ON MATCH SET r.sources = CASE WHEN 'LLM_INFERRED' IN coalesce(r.sources, [coalesce(r.relationSource, 'LLM_INFERRED')]) "
                + "                             THEN coalesce(r.sources, [coalesce(r.relationSource, 'LLM_INFERRED')]) "
                + "                             ELSE coalesce(r.sources, [coalesce(r.relationSource, 'LLM_INFERRED')]) + 'LLM_INFERRED' END, "
                + "                 r.label = coalesce(r.label, $label), "
                + "                 r.updatedAt = datetime()";

        neo4jClient
            .query(linkCypher)
            .bind(normSource)
            .to("source")
            .bind(normTarget)
            .to("target")
            .bind(workspaceId)
            .to("workspaceId")
            .bind(relLabel)
            .to("label")
            .run();
      }
    }
  }

  public void syncWikiLinksToNeo4j(String workspaceId) {
    try {
      java.nio.file.Path wikiDir =
          java.nio.file.Paths.get(wikiBaseDir, workspaceId, "wiki", "concepts");
      if (!java.nio.file.Files.exists(wikiDir)) return;

      java.util.regex.Pattern slugPattern =
          java.util.regex.Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
      java.util.regex.Pattern linkPattern =
          java.util.regex.Pattern.compile("\\[\\[([a-zA-Z0-9_-]+)\\]\\]");

      try (var stream = java.nio.file.Files.list(wikiDir)) {
        List<java.nio.file.Path> files = stream.filter(p -> p.toString().endsWith(".md")).toList();

        // [추가 1] 현재 워크스페이스의 실제 유효 Concept ID만 엄격 수집
        java.util.Set<String> allConceptIds =
            files.stream()
                .map(p -> p.getFileName().toString().replace(".md", "").trim().toLowerCase())
                .filter(id -> slugPattern.matcher(id).matches())
                .collect(java.util.stream.Collectors.toSet());

        for (java.nio.file.Path file : files) {
          String sourceId = file.getFileName().toString().replace(".md", "").trim().toLowerCase();
          if (!slugPattern.matcher(sourceId).matches()) continue;

          String content =
              java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);

          java.util.Set<String> targetIds = new java.util.HashSet<>();

          // 명시적 [[slug]] 크로스 레퍼런스 링크만 파싱 (contains() 단순 매칭 완전 제거!)
          java.util.regex.Matcher m = linkPattern.matcher(content);
          while (m.find()) {
            String targetId = m.group(1).toLowerCase().trim();

            // 자기참조 차단
            if (targetId.equalsIgnoreCase(sourceId)) {
              continue;
            }

            // [추가 2] UNKNOWN_REFERENCE 방어: 대상이 allConceptIds에 없으면 Phantom Node 생성 차단 및 경고
            if (!allConceptIds.contains(targetId)) {
              log.warn(
                  "Phantom node wiki reference rejected in workspace {}: target '{}' from source '{}' does not exist in workspace concepts. Edge skipped.",
                  workspaceId,
                  targetId,
                  sourceId);
              continue;
            }

            targetIds.add(targetId);
          }

          // [추가 3] 단일 [:REFERENCES] 엣지에 EXPLICIT_WIKI_LINK provenance 누적 (중복 생성 차단)
          for (String targetId : targetIds) {
            String linkCypher =
                "MERGE (c:Concept {name: $source, workspaceId: $workspaceId}) "
                    + "MERGE (t:Concept {name: $target, workspaceId: $workspaceId}) "
                    + "MERGE (c)-[r:REFERENCES]->(t) "
                    + "ON CREATE SET r.relationSource = 'EXPLICIT_WIKI_LINK', "
                    + "              r.sources = ['EXPLICIT_WIKI_LINK'], "
                    + "              r.label = 'REFERENCES', "
                    + "              r.createdAt = datetime(), "
                    + "              r.updatedAt = datetime() "
                    + "ON MATCH SET r.sources = CASE WHEN 'EXPLICIT_WIKI_LINK' IN coalesce(r.sources, [coalesce(r.relationSource, 'EXPLICIT_WIKI_LINK')]) "
                    + "                             THEN coalesce(r.sources, [coalesce(r.relationSource, 'EXPLICIT_WIKI_LINK')]) "
                    + "                             ELSE coalesce(r.sources, [coalesce(r.relationSource, 'EXPLICIT_WIKI_LINK')]) + 'EXPLICIT_WIKI_LINK' END, "
                    + "                 r.updatedAt = datetime()";

            neo4jClient
                .query(linkCypher)
                .bind(sourceId)
                .to("source")
                .bind(targetId)
                .to("target")
                .bind(workspaceId)
                .to("workspaceId")
                .run();
          }
        }
      }
      log.info(
          "Synchronized validated wiki cross-reference links into Neo4j graph for workspace: {}",
          workspaceId);
    } catch (Exception e) {
      log.warn("Failed to sync wiki links to Neo4j: {}", e.getMessage());
    }
  }

  public record StructuredWikiPage(
      String id,
      String title,
      String type,
      String summary,
      List<String> tags,
      List<String> aliases,
      String content,
      List<String> relatedConcepts) {}

  public class GraphTools {
    private final String workspaceId;
    private final boolean useNotion;
    private final String modelName;
    public boolean graphUpdated = false;

    public GraphTools(String workspaceId, boolean useNotion, String modelName) {
      this.workspaceId = workspaceId;
      this.useNotion = useNotion;
      this.modelName = modelName;
    }

    @dev.langchain4j.agent.tool.Tool(
        "Search Neo4j graph by keyword and return matching node names/IDs")
    public List<String> searchGraph(String keyword) {
      log.info("Tool [searchGraph] keyword: {}", keyword);
      String cypher =
          "MATCH (c:Concept {workspaceId: $workspaceId}) "
              + "WHERE toLower(c.name) CONTAINS toLower($keyword) OR toLower(c.title) CONTAINS toLower($keyword) "
              + "RETURN c.name as name LIMIT 10";
      java.util.Collection<Map<String, Object>> results =
          neo4jClient
              .query(cypher)
              .bind(workspaceId)
              .to("workspaceId")
              .bind(keyword)
              .to("keyword")
              .fetch()
              .all();

      List<String> names = new java.util.ArrayList<>();
      for (Map<String, Object> r : results) {
        if (r.get("name") != null) names.add((String) r.get("name"));
      }
      if (names.isEmpty()) {
        return List.of("No nodes found for keyword: " + keyword);
      }
      return names;
    }

    @dev.langchain4j.agent.tool.Tool(
        "Get relationships and neighbor node IDs for a specific node ID")
    public String getNodeContext(String nodeId) {
      log.info("Tool [getNodeContext] nodeId: {}", nodeId);
      NotificationController.broadcastNotification(
          workspaceId, "ai_reading", Map.of("nodeId", nodeId));
      String cypher =
          "MATCH (c:Concept {name: $nodeId, workspaceId: $workspaceId})-[r]-(neighbor:Concept) "
              + "RETURN type(r) as relType, neighbor.name as neighborName, startNode(r) = c as isOut LIMIT 20";
      java.util.Collection<Map<String, Object>> results =
          neo4jClient
              .query(cypher)
              .bind(workspaceId)
              .to("workspaceId")
              .bind(nodeId)
              .to("nodeId")
              .fetch()
              .all();

      if (results.isEmpty()) return "No relationships found for " + nodeId;

      StringBuilder sb = new StringBuilder("Relationships for ").append(nodeId).append(":\n");
      for (Map<String, Object> r : results) {
        String relType = (String) r.get("relType");
        String neighborName = (String) r.get("neighborName");
        Boolean isOut = (Boolean) r.get("isOut");
        if (Boolean.TRUE.equals(isOut)) {
          sb.append(" - [").append(relType).append("]-> ").append(neighborName).append("\n");
        } else {
          sb.append(" <-[").append(relType).append("]- ").append(neighborName).append("\n");
        }
      }
      return sb.toString();
    }

    @dev.langchain4j.agent.tool.Tool("Read the content of a markdown wiki page by node ID")
    public String readWikiPage(String nodeId) {
      log.info("Tool [readWikiPage] nodeId: {}", nodeId);
      NotificationController.broadcastNotification(
          workspaceId, "ai_reading", Map.of("nodeId", nodeId));
      String[] subDirs = {"concepts", "entities", "insights"};
      for (String subDir : subDirs) {
        java.nio.file.Path path =
            java.nio.file.Paths.get(wikiBaseDir, workspaceId, "wiki", subDir, nodeId + ".md");
        if (java.nio.file.Files.exists(path)) {
          try {
            return java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
          } catch (Exception e) {
            return "Failed to read wiki page: " + e.getMessage();
          }
        }
      }
      return "Wiki page not found for ID: " + nodeId;
    }

    @dev.langchain4j.agent.tool.Tool(
        "Read the contents of multiple markdown wiki pages by comma-separated node IDs (e.g. 'node-a,node-b')")
    public String readWikiPages(String nodeIds) {
      log.info("Tool [readWikiPages] nodeIds: {}", nodeIds);
      if (nodeIds == null || nodeIds.isBlank()) return "No nodeIds provided.";
      String[] ids = nodeIds.split(",");
      StringBuilder sb = new StringBuilder();
      for (String rawId : ids) {
        String nodeId = rawId.trim();
        if (nodeId.isEmpty()) continue;
        sb.append("=== Wiki Page: ").append(nodeId).append(" ===\n");
        String pageContent = readWikiPage(nodeId);
        sb.append(pageContent).append("\n\n");
      }
      return sb.toString();
    }

    @dev.langchain4j.agent.tool.Tool(
        "Search and read relevant Notion context by query or read specific page if available")
    public String readNotionPage(String query) {
      log.info("Tool [readNotionPage] query: {}", query);
      if (!useNotion) {
        return "Notion access is toggled off. You cannot use this tool right now.";
      }

      com.aimsgraph.domain.workspace.WorkspaceCredentials creds =
          credentialsService.getCredentials(workspaceId);
      String apiKey = creds != null ? creds.getNotionApiKey() : null;
      if (apiKey == null || apiKey.isBlank()) {
        apiKey = System.getenv("NOTION_API_KEY");
      }
      if (apiKey == null || apiKey.isBlank()) {
        return "Notion API key is missing. Cannot access Notion.";
      }

      try {
        String targetPageId = notionIngestService.searchNotionPageId(query, apiKey);

        if (targetPageId != null && !targetPageId.isEmpty()) {
          return notionIngestService.fetchNotionPageText(targetPageId, apiKey);
        } else {
          return "No matching Notion page found for query: " + query;
        }
      } catch (Exception e) {
        return "Failed to fetch Notion content: " + e.getMessage();
      }
    }

    @dev.langchain4j.agent.tool.Tool(
        "Save or build new knowledge into the Second Brain based on user's request. Pass the specific topic and the detailed raw context.")
    public String saveToSecondBrain(String topic, String rawContent) {
      log.info("Tool [saveToSecondBrain] topic: {}", topic);
      try {
        LlmService.this.analyzeTextWithOpenAI(rawContent, workspaceId, modelName);
        this.graphUpdated = true;
        return "Successfully saved '"
            + topic
            + "' to Second Brain. Please inform the user that the graph has been automatically updated.";
      } catch (Exception e) {
        log.error("Failed to save to Second Brain", e);
        return "Failed to save to Second Brain: " + e.getMessage();
      }
    }
  }

  public record AgentResponse(String answer, boolean graphUpdated) {}

  interface GraphAgent {
    @dev.langchain4j.service.SystemMessage(
        "You are a Second Brain Zettelkasten assistant equipped with Agentic Graph Traversal.\n"
            + "Your goal is to accurately answer user queries by exploring the knowledge graph, reading wiki pages, and interacting with Notion (if enabled).\n"
            + "Follow these steps:\n"
            + "1. Extract key concepts from the user's query.\n"
            + "2. Use `searchGraph` to find relevant starting node IDs.\n"
            + "3. Use `getNodeContext` to see relationships of important nodes.\n"
            + "4. Use `readWikiPages` (comma-separated node IDs) instead of calling `readWikiPage` multiple times if you need to read multiple wiki pages. This prevents hitting the execution limit.\n"
            + "5. If Notion access is enabled and relevant, use `readNotionPage` to get additional context.\n"
            + "6. Synthesize all gathered information into a comprehensive answer.\n"
            + "7. IF the user requests to save, build, or record new knowledge/concepts into the Second Brain:\n"
            + "   - You MUST use the `saveToSecondBrain` tool.\n"
            + "   - The `rawContent` parameter MUST be the full, detailed, original source text. DO NOT summarize it.\n"
            + "   - After a successful save, inform the user that the graph has been updated automatically.\n"
            + "8. ALWAYS reply in Korean.\n"
            + "9. You MUST prioritize utilizing the `readWikiPages` tool to bulk-read pages rather than calling single-page read multiple times. NEVER call `readWikiPage` more than 3 times sequentially.")
    String chat(@dev.langchain4j.service.UserMessage String userMessage);
  }

  // ---------------------------------------------------------
  // GRAPH QUERY PIPELINE
  // ---------------------------------------------------------
  public AgentResponse query(
      String workspaceId, String userQuery, String modelName, boolean useNotion) {
    log.info("Executing agentic query for workspace: {}", workspaceId);
    String apiKey = getApiKey(workspaceId);
    if (apiKey == null || apiKey.isEmpty() || apiKey.equals("demo")) {
      return new AgentResponse("API Key is missing or invalid. Please check your settings.", false);
    }

    try {
      ChatModel model = getOrCreateModel(workspaceId, modelName);
      GraphTools tools = new GraphTools(workspaceId, useNotion, modelName);

      GraphAgent agent =
          dev.langchain4j.service.AiServices.builder(GraphAgent.class)
              .chatModel(model)
              .chatMemory(dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(10))
              .tools(tools)
              .maxToolCallingRoundTrips(50)
              .build();

      String awarenessPrompt;
      if (useNotion) {
        awarenessPrompt =
            "You are a helpful assistant integrated with the Second Brain Wiki and Notion via backend MCP.\n"
                + "IMPORTANT RULE: DO NOT announce or explicitly state that you have access to Notion UNLESS the user explicitly asks 'Do you have access?'.\n"
                + "Just seamlessly use the provided tools to answer the question naturally.\n\n";
      } else {
        awarenessPrompt =
            "You are a helpful assistant integrated with the Second Brain Wiki. Currently, Notion access is TOGGLED OFF.\n"
                + "IMPORTANT RULE: DO NOT announce your Notion access status unless explicitly asked. If asked, explain that they must toggle the Notion button ON.\n"
                + "Do not use the readNotionPage tool.\n\n";
      }

      String answer = agent.chat(awarenessPrompt + "[User Query]\n" + userQuery);
      return new AgentResponse(answer, tools.graphUpdated);
    } catch (Exception e) {
      log.error("Failed to query with agentic traversal, falling back to direct query", e);
      return new AgentResponse(
          queryDirect(workspaceId, "[User Query]\n" + userQuery, modelName), false);
    }
  }

  String queryDirect(String apiKey, String userQuery) {
    try {
      java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
      String jsonBody =
          "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\""
              + userQuery.replace("\"", "\\\"").replace("\n", "\\n")
              + "\"}]}";
      java.net.http.HttpRequest request =
          java.net.http.HttpRequest.newBuilder()
              .uri(java.net.URI.create("https://api.openai.com/v1/chat/completions"))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + apiKey)
              .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();

      java.net.http.HttpResponse<String> response =
          client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> respMap =
          mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
      List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
      if (choices != null && !choices.isEmpty()) {
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
      }
      return "Failed to get a response from AI.";
    } catch (Exception e) {
      log.error("Failed to query OpenAI directly", e);
      return "Error calling AI API: " + e.getMessage();
    }
  }

  public record GraphNode(
      String id, String name, String type, String summary, int val, String snippet) {}

  public record GraphLink(String source, String target, String label) {}

  public record KnowledgeGraphResponse(List<GraphNode> nodes, List<GraphLink> links) {}

  public record ExtractedConceptsResponse(List<ExtractedConcept> concepts) {}

  interface ConceptExtractor {
    @dev.langchain4j.service.SystemMessage(
        "You are a Second Brain Zettelkasten concept extraction agent. "
            + "Analyze the following text and extract ONLY core concepts that are genuinely supported by the source "
            + "and deserve their own dedicated wiki page. "
            + "EXTRACTION & FILTERING RULES: "
            + "1) There is NO required target range or minimum number of concepts. Prefer fewer high-confidence concepts over filling a quota. "
            + "2) As a guideline, return approximately 3-15 concepts when the source contains enough information density, but return fewer when fewer concepts are genuinely supported. "
            + "3) Exclude incidental proper nouns and one-time mentions. "
            + "4) Include a proper noun ONLY when the source meaningfully explains its role, mechanism, architecture, usage, or significance. "
            + "5) Exclude trivial keywords, fleeting terms, or jargon that cannot sustain a standalone explanation page. "
            + "6) Every extracted concept must be substantively supported by the source text. Never invent concepts merely to satisfy a count. "
            + "7) Use lowercase english-slug-format with hyphens for all concept IDs (e.g. 'event-driven-architecture').")
    ExtractedConceptsResponse extract(@dev.langchain4j.service.UserMessage String content);
  }

  interface UnifiedGraphExtractor {
    @dev.langchain4j.service.SystemMessage(
        "You are a structured data architect specializing in Zettelkasten-style Second Brain systems. "
            + "Your sole job is to read the given document(s), identify their core knowledge structure, "
            + "and output a single strictly validated JSON knowledge graph that UNIFIES all documents. "
            + "CRITICAL EXTRACTION RULES: "
            + "1) There is NO required target range or minimum number of concepts. Prefer fewer high-confidence concepts over filling a quota. "
            + "2) As a guideline, return approximately 3-15 concepts when the source contains enough information density, but return fewer when fewer concepts are genuinely supported. "
            + "3) Only extract concepts that are SUBSTANTIVELY discussed in the source — not merely mentioned in passing. "
            + "4) Exclude incidental proper nouns and one-time mentions. "
            + "5) Include a proper noun ONLY when the source meaningfully explains its role, mechanism, architecture, usage, or significance. "
            + "6) Do NOT create nodes for: example project names, one-off tool mentions, file names, or UI element names unless they are the MAIN TOPIC. "
            + "7) Use lowercase english-slug-format with hyphens for all node IDs (e.g. 'event-driven-architecture'). "
            + "8) You never hallucinate IDs. "
            + "9) RELATIONSHIP & LINK RULES (PRECISION OVER DENSITY): "
            + "Only create a link when the relationship between two concepts is explicitly supported by the source content or by a clear, well-established technical relationship directly relevant to the concept. "
            + "Allowed relationship types for `label` include: 'DEPENDENCY' (A depends on B), 'COMPOSITION' (A contains B), 'IMPLEMENTATION' (A implements/uses B), 'DIRECT_INTERACTION' (A interacts with B), 'EXPLICIT_REFERENCE' (A references B). "
            + "DO NOT create links merely because: the concepts belong to the same broad technology domain, they are commonly used together, one concept appears as an example, their names appear in the same document, or they could theoretically be used together. "
            + "If no sufficiently strong relationship exists between concepts, return an empty `links: []` array. "
            + "An isolated node with no links is a valid, normal, and expected result. "
            + "Never invent a relationship merely to avoid an empty links array. Precision is strictly more important than graph connectivity.")
    KnowledgeGraphResponse extract(@dev.langchain4j.service.UserMessage String userPrompt);
  }
}

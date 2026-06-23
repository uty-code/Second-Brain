package com.aimsgraph.ingest;

import com.aimsgraph.domain.workspace.WorkspaceService;
import com.aimsgraph.domain.workspace.WorkspaceCredentialsService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import com.aimsgraph.api.NotificationController;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    @org.springframework.beans.factory.annotation.Value("${llm.api-key:demo}")
    private String defaultApiKey;

    @org.springframework.beans.factory.annotation.Value("${wiki.base.dir:workspaces}")
    private String wikiBaseDir;

    private final WorkspaceService workspaceService;
    private final WorkspaceCredentialsService credentialsService;
    private final Neo4jClient neo4jClient;
    private final NotionIngestService notionIngestService;
    private final Map<String, ChatLanguageModel> modelCache = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.locks.ReentrantLock> fileLocks = new ConcurrentHashMap<>();

    private java.util.concurrent.locks.ReentrantLock getFileLock(String filePath) {
        return fileLocks.computeIfAbsent(filePath, k -> new java.util.concurrent.locks.ReentrantLock());
    }

    private String getApiKey(String workspaceId) {
        String key = System.getenv("OPENAI_API_KEY");
        if (key != null && !key.isEmpty()) return key;
        return defaultApiKey;
    }

    private ChatLanguageModel getOrCreateModel(String workspaceId, String modelName) {
        String cacheKey = workspaceId + ":" + (modelName != null ? modelName : "gpt-4o-mini");
        return modelCache.computeIfAbsent(cacheKey, k -> {
            if ("deepseek-v4".equalsIgnoreCase(modelName)) {
                log.info("Initializing OpenAiChatModel (OpenRouter - DeepSeek) for workspace: {}", workspaceId);
                String envDeepSeekKey = System.getenv("DEEPSEEK_API_KEY");
                com.aimsgraph.domain.workspace.WorkspaceCredentials creds = credentialsService.getCredentials(workspaceId);
                String deepseekKey = creds != null ? creds.getDeepseekApiKey() : null;
                String finalKey = (deepseekKey != null && !deepseekKey.isBlank()) ? deepseekKey : envDeepSeekKey;
                if (finalKey == null || finalKey.isBlank()) {
                    log.warn("DeepSeek API Key is missing. Fallback to OpenAI API Key (might fail if not supported).");
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
            ChatLanguageModel model = getOrCreateModel(workspaceId, modelName);
            return model.generate(userQuery);
        } catch (Exception e) {
            log.error("Direct query failed", e);
            return "Error: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------
    // TEXT INGESTION PIPELINE (IngestionWorker)
    // ---------------------------------------------------------
    public List<ExtractedConcept> extractKnowledge(String eventId, String content, String workspaceId) {
        log.info("Extracting knowledge graph for text in workspace: {}", workspaceId);
        
        ChatLanguageModel model = getOrCreateModel(workspaceId, "gpt-4o-mini");

        String prompt = "You are a Second Brain Zettelkasten assistant.\n" +
                "Analyze the following text and extract multiple atomic concepts or entities.\n" +
                "You MUST output ONLY a valid JSON Array. Do not use Markdown fences like ```json.\n" +
                "Each object in the array MUST have the following keys:\n" +
                "  \"name\": string (english slug with hyphens, e.g. 'llm-wiki')\n" +
                "  \"title\": string (korean title)\n" +
                "  \"type\": string ('concept' or 'entity')\n" +
                "  \"tags\": array of strings\n" +
                "  \"aliases\": array of strings\n" +
                "  \"summary\": string (one line summary in korean)\n" +
                "  \"linkedConcepts\": array of objects, where each object MUST have 'name' (string) and 'type' (string, MUST BE ONE OF: EXTENDS, CONTRADICTS, DEPENDS_ON, EXPLAINS, RELATED_TO)\n\n" +
                "Text to analyze:\n" + content;

        String response = model.generate(prompt);
        response = response.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("\\s*```$", "");
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response, new TypeReference<List<ExtractedConcept>>() {});
        } catch (Exception e) {
            log.error("Failed to parse LLM response into concepts", e);
            throw new RuntimeException("Failed to parse concepts from LLM", e);
        }
    }

    // ---------------------------------------------------------
    // FILE INGESTION PIPELINE (AnalyzeController)
    // ---------------------------------------------------------
    public Map<String, Object> analyzeFilesWithOpenAI(org.springframework.web.multipart.MultipartFile[] files, String workspaceId, String modelName) throws Exception {
        if (getApiKey(workspaceId) == null || getApiKey(workspaceId).isEmpty() || getApiKey(workspaceId).equals("demo")) {
            throw new RuntimeException("OpenAI API Key is not configured.");
        }

        StringBuilder rawSourceBuilder = new StringBuilder();
        for (org.springframework.web.multipart.MultipartFile file : files) {
            if (!file.isEmpty()) {
                rawSourceBuilder.append("--- File: ").append(file.getOriginalFilename()).append(" ---\n");
                rawSourceBuilder.append(new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8)).append("\n\n");
            }
        }

        if (rawSourceBuilder.length() == 0) {
            throw new RuntimeException("업로드된 파일이 모두 비어있습니다.");
        }
        
        String rawSourceText = rawSourceBuilder.toString();
        log.info("Files parsed into text ({} bytes). Routing to analyzeTextWithOpenAI...", rawSourceText.length());

        // Delegate to analyzeTextWithOpenAI using the extracted text
        return analyzeTextWithOpenAI(rawSourceText, workspaceId, modelName);
    }

    public Map<String, Object> analyzeTextWithOpenAI(String rawSourceText, String workspaceId, String modelName) throws Exception {
        if (getApiKey(workspaceId) == null || getApiKey(workspaceId).isEmpty() || getApiKey(workspaceId).equals("demo")) {
            throw new RuntimeException("OpenAI API Key is not configured.");
        }

        // === 1단계: 파일 대신 텍스트로 분석하여 그래프(Nodes, Links) 추출 ===
        String analysisResult = callResponsesAPI(null, rawSourceText, workspaceId, modelName);
        log.info("Analysis complete for raw text. Parsing graph data...");

        String cleaned = analysisResult.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("\\s*```$", "");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> graphData = mapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
        
        // === 2단계: 동기식 & 병렬 위키 생성 ===
        generateWikiPages(graphData, rawSourceText, workspaceId, modelName);
        
        // Neo4j에 그래프(노드와 링크) 저장
        saveGraphToNeo4j(graphData, workspaceId);
        
        return graphData;
    }

    private String uploadFileToOpenAI(org.springframework.web.multipart.MultipartFile file, String workspaceId) throws Exception {
        String boundary = "----FormBoundary" + java.util.UUID.randomUUID().toString().replace("-", "");
        byte[] fileBytes = file.getBytes();
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        baos.write(("--" + boundary + "\r\n").getBytes());
        baos.write("Content-Disposition: form-data; name=\"purpose\"\r\n\r\n".getBytes());
        baos.write("user_data\r\n".getBytes());
        baos.write(("--" + boundary + "\r\n").getBytes());
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes());
        baos.write(("Content-Type: " + (file.getContentType() != null ? file.getContentType() : "application/octet-stream") + "\r\n\r\n").getBytes());
        baos.write(fileBytes);
        baos.write(("\r\n--" + boundary + "--\r\n").getBytes());
        byte[] bodyBytes = baos.toByteArray();

        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.openai.com/v1/files"))
                .header("Authorization", "Bearer " + getApiKey(workspaceId))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build();

        java.net.http.HttpResponse<String> resp = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> respMap = mapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
        
        if (respMap.get("error") != null) {
            throw new RuntimeException("OpenAI file upload failed: " + respMap.get("error"));
        }
        return (String) respMap.get("id");
    }

    private String callResponsesAPI(java.util.List<String> fileIds, String rawSourceText, String workspaceId, String modelName) throws Exception {
        java.util.Collection<Map<String, Object>> existingNodes = neo4jClient.query("MATCH (n:Concept {workspaceId: $workspaceId}) RETURN n.name as name LIMIT 200")
                .bind(workspaceId).to("workspaceId").fetch().all();
        List<String> existingNames = new java.util.ArrayList<>();
        for (Map<String, Object> record : existingNodes) {
            if (record.get("name") != null) existingNames.add((String) record.get("name"));
        }
        String existingConceptsStr = existingNames.isEmpty() ? "None" : String.join(", ", existingNames);

        String systemPrompt = "You are a structured data architect specializing in Zettelkasten-style Second Brain systems. "
                + "Your sole job is to read the given document(s), identify their core knowledge structure, "
                + "and output a single strictly validated JSON knowledge graph that UNIFIES all documents. "
                + "You never hallucinate IDs. You never output anything other than valid JSON. Do NOT use markdown code blocks like ```json.";

        String userPrompt = "Here are the existing concepts already in the knowledge graph: [" + existingConceptsStr + "]\n"
                + "CRITICAL RULE: If a concept in the document strongly matches or relates to an existing concept, you MUST reuse the exact same ID. Only generate new IDs for entirely new concepts.\n\n"
                + "Analyze the attached document(s) and build a single UNIFIED knowledge graph.\n\n"
                + "Return ONLY a single valid JSON object.\n"
                + "Example structure:\n"
                + "{\"nodes\":[{\"id\":\"english-slug\",\"name\":\"Korean Name\",\"type\":\"concept\",\"summary\":\"One sentence summary in Korean\",\"val\":5,\"snippet\":\"1~2 sentences exact quote from the document explaining this concept\"}],"
                + "\"links\":[{\"source\":\"node-id-a\",\"target\":\"node-id-b\",\"label\":\"RELATED_TO\"}]}\n\n"
                + "Raw Document Content:\n" + rawSourceText;

        ChatLanguageModel model = getOrCreateModel(workspaceId, modelName != null ? modelName : "gpt-4o-mini");
        
        log.info("Calling Language Model for unified knowledge graph extraction...");
        String response = model.generate(
                dev.langchain4j.data.message.SystemMessage.from(systemPrompt), 
                dev.langchain4j.data.message.UserMessage.from(userPrompt)
        ).content().text();
        
        return response;
    }

    // ---------------------------------------------------------
    // PARALLEL WIKI GENERATION
    // ---------------------------------------------------------
    public void generateWikiPages(Map<String, Object> graphData, String rawSourceText, String workspaceId, String modelName) {
        java.util.List<Map<String, Object>> nodes = (java.util.List<Map<String, Object>>) graphData.get("nodes");
        if (nodes == null || nodes.isEmpty()) return;

        java.util.Map<String, java.util.List<String>> edgeMap = new java.util.HashMap<>();
        java.util.List<Map<String, Object>> links = (java.util.List<Map<String, Object>>) graphData.get("links");
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
            java.nio.file.Path wikiDir = java.nio.file.Paths.get(wikiBaseDir, workspaceId, "wiki", "concepts");
            java.nio.file.Files.createDirectories(wikiDir);

            ObjectMapper mapper = new ObjectMapper();

            String systemPrompt = "You are a specialized agent for writing structured Markdown Wiki pages for a Zettelkasten-style Second Brain system.\n\n"
                    + "## Output Rules\n"
                    + "- ALL content (title, summary, body) MUST be written in Korean.\n"
                    + "- Cross-references to other concepts MUST use `[[Concept Name]]` syntax.\n"
                    + "- The `id` field MUST be an english slug with hyphens (e.g. `event-driven-architecture`).\n\n"
                    + "## Quality Standards\n"
                    + "The wiki page `content` field MUST include:\n"
                    + "1. **정의**: 개념 명확 서술\n"
                    + "2. **핵심 구성 요소**: 구조화된 설명\n"
                    + "3. **동작 원리**: 단계별 프로세스\n"
                    + "4. **장점과 트레이드오프**: 한계 포함 균형 서술\n"
                    + "5. **실전 적용 사례**: 코드나 시나리오\n\n"
                    + "## Format & Quality\n"
                    + "- Use markdown elements such as tables and bullet points to structure the document precisely and beautifully.\n"
                    + "## Summary 작성 규칙\n"
                    + "- summary 필드는 30~80자 사이의 한 문장으로 압축.\n";

            Map<String, Object> responseFormat = Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                    "name", "wiki_page",
                    "strict", true,
                    "schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "id", Map.of("type", "string"),
                            "title", Map.of("type", "string"),
                            "type", Map.of("type", "string"),
                            "summary", Map.of("type", "string"),
                            "tags", Map.of("type", "array", "items", Map.of("type", "string")),
                            "aliases", Map.of("type", "array", "items", Map.of("type", "string")),
                            "content", Map.of("type", "string"),
                            "relatedConcepts", Map.of(
                                  "type", "array",
                                  "items", Map.of("type", "string")
                            )
                        ),
                        "required", List.of("id", "title", "type", "summary", "tags", "aliases", "content", "relatedConcepts"),
                        "additionalProperties", false
                    )
                )
            );

            // API Rate Limiting 보호를 위해 동시 실행 10개로 제한
            java.util.concurrent.Semaphore rateLimiter = new java.util.concurrent.Semaphore(10);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
                java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
                
                for (Map<String, Object> node : nodes) {
                    futures.add(executor.submit(() -> {
                        try {
                            rateLimiter.acquire();
                            try {
                                String nodeJson = mapper.writeValueAsString(node);
                                String snippet = (String) node.get("snippet");
                                String userPrompt = "Here is the ORIGINAL SOURCE TEXT provided by the user:\n"
                                        + "==============================\n"
                                        + rawSourceText + "\n"
                                        + "==============================\n\n"
                                        + "And here is the EXACT QUOTE (Snippet) where this specific concept is mentioned:\n"
                                        + ">>> " + snippet + " <<<\n\n"
                                        + "Based on the ENTIRE source text for deep context, but FOCUSING HEAVILY on the exact quote above, generate a detailed, high-quality wiki page for THIS SPECIFIC concept:\n"
                                        + nodeJson + "\n\n"
                                        + "Maximize depth, accuracy, and capture the author's original intent from the surrounding context. DO NOT hallucinate outside knowledge.";

                                java.util.List<Map<String, Object>> messages = new java.util.ArrayList<>();
                                messages.add(Map.of("role", "system", "content", systemPrompt));
                                messages.add(Map.of("role", "user", "content", userPrompt));

                                String requestModelName = "gpt-4o-mini";
                                String apiUrl = "https://api.openai.com/v1/chat/completions";
                                String apiKey = getApiKey(workspaceId);

                                if ("deepseek-v4".equalsIgnoreCase(modelName)) {
                                    requestModelName = "deepseek/deepseek-v4-pro";
                                    apiUrl = "https://openrouter.ai/api/v1/chat/completions";
                                    
                                    String envDeepSeekKey = System.getenv("DEEPSEEK_API_KEY");
                                    com.aimsgraph.domain.workspace.WorkspaceCredentials creds = credentialsService.getCredentials(workspaceId);
                                    String deepseekKey = creds != null ? creds.getDeepseekApiKey() : null;
                                    String finalKey = (deepseekKey != null && !deepseekKey.isBlank()) ? deepseekKey : envDeepSeekKey;
                                    if (finalKey != null && !finalKey.isBlank()) {
                                        apiKey = finalKey;
                                    }
                                }

                                 java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
                                 requestBody.put("model", requestModelName);
                                 requestBody.put("messages", messages);
                                 requestBody.put("response_format", responseFormat);
                                 if ("deepseek-v4".equalsIgnoreCase(modelName)) {
                                     requestBody.put("max_tokens", 4000);
                                 }

                                String jsonBody = mapper.writeValueAsString(requestBody);
                                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                                        .uri(java.net.URI.create(apiUrl))
                                        .header("Content-Type", "application/json")
                                        .header("Authorization", "Bearer " + apiKey)
                                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                                        .build();

                                java.net.http.HttpResponse<String> resp = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                                Map<String, Object> respMap = mapper.readValue(resp.body(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                                
                                if (respMap.get("error") != null) {
                                    log.error("Wiki generation failed for node {}: {}", node.get("name"), respMap.get("error"));
                                    return;
                                }

                                String outputText = "";
                                java.util.List<Map<String, Object>> choices = (java.util.List<Map<String, Object>>) respMap.get("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                                    if (message != null) {
                                        outputText = (String) message.get("content");
                                    }
                                }

                                String cleaned = outputText.trim()
                                        .replaceAll("^```json\\s*", "")
                                        .replaceAll("^```\\s*", "")
                                        .replaceAll("\\s*```$", "");

                                StructuredWikiPage page = mapper.readValue(cleaned, StructuredWikiPage.class);

                                if (page != null && page.id() != null && !page.id().isBlank()) {
                                    java.util.List<String> actualLinks = edgeMap.getOrDefault(page.id(), new java.util.ArrayList<>());
                                    java.util.List<String> validRelatedConcepts = actualLinks.stream().distinct().toList();
                                    
                                    page = new StructuredWikiPage(
                                        page.id(), page.title(), page.type(), page.summary(), page.tags(), page.aliases(), page.content(), validRelatedConcepts
                                    );

                                    StringBuilder sb = new StringBuilder();
                                    sb.append("---\n");
                                    sb.append("title: ").append(page.title() != null ? page.title() : "").append("\n");
                                    sb.append("type: ").append(page.type() != null ? page.type() : "").append("\n");
                                    sb.append("created_at: ").append(java.time.LocalDate.now().toString()).append("\n");
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
                                    
                                    java.util.concurrent.locks.ReentrantLock lock = getFileLock(filePath.toAbsolutePath().toString());
                                    lock.lock();
                                    try {
                                        if (java.nio.file.Files.exists(filePath)) {
                                            String existingContent = java.nio.file.Files.readString(filePath, java.nio.charset.StandardCharsets.UTF_8);
                                            String appendedContent = existingContent + "\n## 추가 정보 (업데이트 됨)\n\n" + sb.toString();
                                            java.nio.file.Files.writeString(filePath, appendedContent, java.nio.charset.StandardCharsets.UTF_8);
                                            log.info("Appended to existing wiki page: {}", filename);
                                        } else {
                                            java.nio.file.Files.writeString(filePath, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
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

            log.info("Wiki generation completed successfully! Files saved to {}", wikiDir.toAbsolutePath());

        } catch (Exception e) {
            log.error("Error in generateWikiPages", e);
        }
    }

    void saveGraphToNeo4j(Map<String, Object> graphData, String workspaceId) {
        if (graphData == null) return;
        
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) graphData.get("nodes");
        if (nodes != null) {
            for (Map<String, Object> node : nodes) {
                String id = (String) node.get("id");
                if (id == null || id.isBlank()) continue;
                String title = (String) node.get("name");
                if (title == null) title = id;

                String createNodeCypher = "MERGE (c:Concept {name: $name, workspaceId: $workspaceId}) " +
                        "ON CREATE SET c.title = $title, c.createdAt = datetime() " +
                        "ON MATCH SET c.title = $title, c.updatedAt = datetime()";
                neo4jClient.query(createNodeCypher)
                           .bind(id).to("name")
                           .bind(workspaceId).to("workspaceId")
                           .bind(title).to("title")
                           .run();
            }
        }

        List<Map<String, Object>> links = (List<Map<String, Object>>) graphData.get("links");
        if (links != null) {
            for (Map<String, Object> link : links) {
                String source = (String) link.get("source");
                String target = (String) link.get("target");
                if (source == null || source.isBlank() || target == null || target.isBlank()) continue;

                String relType = (String) link.get("label");
                if (relType == null || relType.isBlank()) {
                    relType = "RELATES_TO";
                } else {
                    relType = relType.toUpperCase().replaceAll("[^A-Z_]", "");
                    if (relType.isEmpty()) relType = "RELATES_TO";
                }

                String linkCypher = "MERGE (c:Concept {name: $name, workspaceId: $workspaceId}) " +
                                    "MERGE (l:Concept {name: $link, workspaceId: $workspaceId}) " +
                                    "MERGE (c)-[:" + relType + "]->(l)";
                neo4jClient.query(linkCypher)
                           .bind(source).to("name")
                           .bind(target).to("link")
                           .bind(workspaceId).to("workspaceId")
                           .run();
            }
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
        List<String> relatedConcepts
    ) {}

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

        @dev.langchain4j.agent.tool.Tool("Search Neo4j graph by keyword and return matching node names/IDs")
        public List<String> searchGraph(String keyword) {
            log.info("Tool [searchGraph] keyword: {}", keyword);
            String cypher = "MATCH (c:Concept {workspaceId: $workspaceId}) " +
                            "WHERE toLower(c.name) CONTAINS toLower($keyword) OR toLower(c.title) CONTAINS toLower($keyword) " +
                            "RETURN c.name as name LIMIT 10";
            java.util.Collection<Map<String, Object>> results = neo4jClient.query(cypher)
                    .bind(workspaceId).to("workspaceId")
                    .bind(keyword).to("keyword")
                    .fetch().all();
            
            List<String> names = new java.util.ArrayList<>();
            for (Map<String, Object> r : results) {
                if (r.get("name") != null) names.add((String) r.get("name"));
            }
            if (names.isEmpty()) {
                return List.of("No nodes found for keyword: " + keyword);
            }
            return names;
        }

        @dev.langchain4j.agent.tool.Tool("Get relationships and neighbor node IDs for a specific node ID")
        public String getNodeContext(String nodeId) {
            log.info("Tool [getNodeContext] nodeId: {}", nodeId);
            NotificationController.broadcastNotification(workspaceId, "ai_reading", Map.of("nodeId", nodeId));
            String cypher = "MATCH (c:Concept {name: $nodeId, workspaceId: $workspaceId})-[r]-(neighbor:Concept) " +
                            "RETURN type(r) as relType, neighbor.name as neighborName, startNode(r) = c as isOut LIMIT 20";
            java.util.Collection<Map<String, Object>> results = neo4jClient.query(cypher)
                    .bind(workspaceId).to("workspaceId")
                    .bind(nodeId).to("nodeId")
                    .fetch().all();
            
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
            NotificationController.broadcastNotification(workspaceId, "ai_reading", Map.of("nodeId", nodeId));
            String[] subDirs = {"concepts", "entities", "insights"};
            for (String subDir : subDirs) {
                java.nio.file.Path path = java.nio.file.Paths.get(wikiBaseDir, workspaceId, "wiki", subDir, nodeId + ".md");
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

        @dev.langchain4j.agent.tool.Tool("Search and read relevant Notion context by query or read specific page if available")
        public String readNotionPage(String query) {
            log.info("Tool [readNotionPage] query: {}", query);
            if (!useNotion) {
                return "Notion access is toggled off. You cannot use this tool right now.";
            }
            
            com.aimsgraph.domain.workspace.WorkspaceCredentials creds = credentialsService.getCredentials(workspaceId);
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

        @dev.langchain4j.agent.tool.Tool("Save or build new knowledge into the Second Brain based on user's request. Pass the specific topic and the detailed raw context.")
        public String saveToSecondBrain(String topic, String rawContent) {
            log.info("Tool [saveToSecondBrain] topic: {}", topic);
            try {
                LlmService.this.analyzeTextWithOpenAI(rawContent, workspaceId, modelName);
                this.graphUpdated = true;
                return "Successfully saved '" + topic + "' to Second Brain. Please inform the user that the graph has been automatically updated.";
            } catch (Exception e) {
                log.error("Failed to save to Second Brain", e);
                return "Failed to save to Second Brain: " + e.getMessage();
            }
        }
    }

    public record AgentResponse(String answer, boolean graphUpdated) {}

    interface GraphAgent {
        @dev.langchain4j.service.SystemMessage(
            "You are a Second Brain Zettelkasten assistant equipped with Agentic Graph Traversal.\n" +
            "Your goal is to accurately answer user queries by exploring the knowledge graph, reading wiki pages, and interacting with Notion (if enabled).\n" +
            "Follow these steps:\n" +
            "1. Extract key concepts from the user's query.\n" +
            "2. Use `searchGraph` to find relevant starting node IDs.\n" +
            "3. Use `getNodeContext` to see relationships of important nodes.\n" +
            "4. Use `readWikiPage` to read the actual content of the nodes to gather facts.\n" +
            "5. If Notion access is enabled and relevant, use `readNotionPage` to get additional context.\n" +
            "6. Synthesize all gathered information into a comprehensive answer.\n" +
            "7. IF the user requests to save, build, or record new knowledge/concepts into the Second Brain:\n" +
            "   - You MUST use the `saveToSecondBrain` tool.\n" +
            "   - The `rawContent` parameter MUST be the full, detailed, original source text. DO NOT summarize it.\n" +
            "   - After a successful save, inform the user that the graph has been updated automatically.\n" +
            "8. ALWAYS reply in Korean."
        )
        String chat(@dev.langchain4j.service.UserMessage String userMessage);
    }

    // ---------------------------------------------------------
    // GRAPH QUERY PIPELINE
    // ---------------------------------------------------------
    public AgentResponse query(String workspaceId, String userQuery, String modelName, boolean useNotion) {
        log.info("Executing agentic query for workspace: {}", workspaceId);
        String apiKey = getApiKey(workspaceId);
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("demo")) {
            return new AgentResponse("API Key is missing or invalid. Please check your settings.", false);
        }

        try {
            ChatLanguageModel model = getOrCreateModel(workspaceId, modelName);
            GraphTools tools = new GraphTools(workspaceId, useNotion, modelName);

            GraphAgent agent = dev.langchain4j.service.AiServices.builder(GraphAgent.class)
                .chatLanguageModel(model)
                .chatMemory(dev.langchain4j.memory.chat.MessageWindowChatMemory.withMaxMessages(10))
                .tools(tools)
                .build();

            String awarenessPrompt;
            if (useNotion) {
                awarenessPrompt = "You are a helpful assistant integrated with the Second Brain Wiki and Notion via backend MCP.\n"
                        + "IMPORTANT RULE: DO NOT announce or explicitly state that you have access to Notion UNLESS the user explicitly asks 'Do you have access?'.\n"
                        + "Just seamlessly use the provided tools to answer the question naturally.\n\n";
            } else {
                awarenessPrompt = "You are a helpful assistant integrated with the Second Brain Wiki. Currently, Notion access is TOGGLED OFF.\n"
                        + "IMPORTANT RULE: DO NOT announce your Notion access status unless explicitly asked. If asked, explain that they must toggle the Notion button ON.\n"
                        + "Do not use the readNotionPage tool.\n\n";
            }

            String answer = agent.chat(awarenessPrompt + "[User Query]\n" + userQuery);
            return new AgentResponse(answer, tools.graphUpdated);
        } catch (Exception e) {
            log.error("Failed to query with agentic traversal, falling back to direct query", e);
            return new AgentResponse(queryDirect(workspaceId, "[User Query]\n" + userQuery, modelName), false);
        }
    }

    String queryDirect(String apiKey, String userQuery) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String jsonBody = "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\"" + userQuery.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]}";
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> respMap = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
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
}


package com.aimsgraph.ingest.eval;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.aimsgraph.domain.workspace.WorkspaceCredentialsService;
import com.aimsgraph.domain.workspace.WorkspaceService;
import com.aimsgraph.ingest.ExtractedConcept;
import com.aimsgraph.ingest.LlmService;
import com.aimsgraph.ingest.LlmService.StructuredWikiPage;
import com.aimsgraph.ingest.NotionIngestService;
import com.aimsgraph.ingest.validator.WikiPageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class LiveOpenAiCorpusEvaluationTest {

  @Mock private WorkspaceService workspaceService;
  @Mock private WorkspaceCredentialsService credentialsService;
  @Mock private Neo4jClient neo4jClient;
  @Mock private NotionIngestService notionIngestService;

  @TempDir Path tempWikiDir;

  private LlmService llmService;
  private WikiPageValidator validator;
  private HallucinationInspector inspector;
  private EvaluationRunner runner;
  private ObjectMapper mapper = new ObjectMapper();
  private String apiKey;

  @BeforeEach
  void setUp() {
    apiKey = System.getenv("OPENAI_API_KEY");
    assumeTrue(
        apiKey != null && !apiKey.isBlank(), "OPENAI_API_KEY 환경변수가 설정되어 있어야 라이브 테스트를 실행합니다.");

    validator = new WikiPageValidator();
    inspector = new HallucinationInspector();
    runner = new EvaluationRunner();

    llmService =
        new LlmService(
            workspaceService, credentialsService, neo4jClient, notionIngestService, validator);

    ReflectionTestUtils.setField(
        llmService, "wikiBaseDir", tempWikiDir.toAbsolutePath().toString());
    ReflectionTestUtils.setField(llmService, "defaultApiKey", apiKey);
    ReflectionTestUtils.setField(
        llmService, "openAiApiUrl", "https://api.openai.com/v1/chat/completions");
  }

  @Test
  @DisplayName("실제 OpenAI API 라이브 호출: 5대 대표 시나리오 실전 개념 추출, 위키 생성, 실시간 검증 및 리포트 생성")
  void evaluateLiveRepresentativeCorpus() throws Exception {
    Path corpusDir = Paths.get("..", "eval", "corpus");
    if (!Files.exists(corpusDir)) {
      corpusDir = Paths.get("eval", "corpus");
    }

    // 5대 대표 시나리오 파일명
    List<String> targetScenarios =
        List.of(
            "01_short_blog_docker",
            "07_arch_transactional_outbox",
            "09_code_spring_security_filter",
            "21_sparse_meeting_memo",
            "23_buzzword_agile_cloud_synergy");

    String workspaceId = "ws-live-eval";
    String runId =
        "live_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

    List<EvaluationResult> results = new ArrayList<>();

    System.out.println("=================================================================");
    System.out.println("🚀 [LIVE EVALUATION] Starting Real OpenAI API Evaluation...");
    System.out.println("=================================================================");

    for (String scenarioId : targetScenarios) {
      Path mdFile = corpusDir.resolve(scenarioId + ".md");
      Path jsonFile = corpusDir.resolve(scenarioId + ".expected.json");

      assertTrue(Files.exists(mdFile), "파일이 존재해야 함: " + mdFile);
      assertTrue(Files.exists(jsonFile), "메타데이터가 존재해야 함: " + jsonFile);

      String rawContent = Files.readString(mdFile, StandardCharsets.UTF_8);
      CorpusExpectedMetadata meta =
          mapper.readValue(jsonFile.toFile(), CorpusExpectedMetadata.class);

      System.out.println("\n-----------------------------------------------------------------");
      System.out.println("▶ Scenario: " + scenarioId + " (" + meta.category() + ")");
      System.out.println("-----------------------------------------------------------------");

      // 1. 실제 LLM 개념 추출 API 호출
      System.out.println("Calling OpenAI API: extractKnowledge...");
      List<ExtractedConcept> extractedConcepts =
          llmService.extractKnowledge("event-" + scenarioId, rawContent, workspaceId);

      List<String> extractedConceptIds = new ArrayList<>();
      List<Map<String, Object>> nodes = new ArrayList<>();

      for (ExtractedConcept c : extractedConcepts) {
        String slug = c.getName() != null ? c.getName().toLowerCase().trim() : "";
        if (!slug.isEmpty()) {
          extractedConceptIds.add(slug);
          Map<String, Object> node = new HashMap<>();
          node.put("id", slug);
          node.put("name", c.getTitle() != null ? c.getTitle() : slug);
          node.put("summary", c.getSummary());
          node.put("snippet", c.getContent());
          nodes.add(node);
        }
      }

      System.out.println(
          "✔ Extracted Concepts (" + extractedConceptIds.size() + "): " + extractedConceptIds);

      // 2. 실제 LLM 위키 페이지 생성 API 호출 (병렬 가상스레드 + WikiPageValidator 실시간 검증/재시도)
      System.out.println("Calling OpenAI API: generateWikiPages (with Validator & Retry Loop)...");
      Map<String, Object> graphData = Map.of("nodes", nodes, "links", Collections.emptyList());
      llmService.generateWikiPages(graphData, rawContent, workspaceId, "gpt-4o-mini");

      // 3. 실제 디렉토리에 저장된 위키 파일 수집 및 검사
      Path conceptsDir = tempWikiDir.resolve(workspaceId).resolve("wiki").resolve("concepts");
      List<StructuredWikiPage> generatedPages = new ArrayList<>();
      int passCount = 0;

      if (Files.exists(conceptsDir)) {
        try (var stream = Files.list(conceptsDir)) {
          List<Path> wikiFiles = stream.filter(p -> p.toString().endsWith(".md")).toList();
          for (Path wp : wikiFiles) {
            String fileContent = Files.readString(wp, StandardCharsets.UTF_8);
            String pageId = wp.getFileName().toString().replace(".md", "");
            // Frontmatter 검증
            var fmResult = validator.validateFrontmatter(fileContent);
            if (fmResult.valid()) {
              passCount++;
            }
            generatedPages.add(
                new StructuredWikiPage(
                    pageId, pageId, "concept", "", List.of(), List.of(), fileContent, List.of()));
          }
        }
      }

      System.out.println("✔ Validated & Saved Wiki Pages: " + generatedPages.size());

      // 4. HallucinationInspector 정밀 대조 검사
      List<HallucinationViolation> violations =
          inspector.inspect(rawContent, meta, extractedConceptIds, generatedPages);

      List<String> matchedExpected = new ArrayList<>();
      if (meta.expectedConcepts() != null) {
        for (String exp : meta.expectedConcepts()) {
          if (extractedConceptIds.contains(exp.toLowerCase())) {
            matchedExpected.add(exp);
          }
        }
      }

      List<String> forbiddenFound = new ArrayList<>();
      if (meta.forbiddenConcepts() != null) {
        for (String forb : meta.forbiddenConcepts()) {
          if (extractedConceptIds.contains(forb.toLowerCase())) {
            forbiddenFound.add(forb);
          }
        }
      }

      System.out.println(
          "✔ Baseline Concepts Matched: "
              + matchedExpected.size()
              + "/"
              + meta.expectedConcepts().size());
      if (!forbiddenFound.isEmpty()) {
        System.out.println("❌ FORBIDDEN CONCEPTS FOUND: " + forbiddenFound);
      }
      if (!violations.isEmpty()) {
        System.out.println("⚠️ Violations/Warnings (" + violations.size() + "):");
        for (HallucinationViolation v : violations) {
          System.out.println("   - [" + v.violationType() + "] " + v.evidenceSnippet());
        }
      }

      results.add(
          new EvaluationResult(
              scenarioId,
              meta.category(),
              extractedConceptIds.size(),
              matchedExpected,
              forbiddenFound,
              violations,
              true,
              false,
              false,
              false,
              0,
              Collections.emptyMap()));
    }

    System.out.println("\n=================================================================");
    System.out.println("📊 Generating Live Evaluation Report & Archiving Results...");
    System.out.println("=================================================================");

    // 5. 마크다운 리포트 생성 및 저장
    String reportMd = runner.generateMarkdownReport(results, runId);

    Path rootDir = Paths.get("..", "eval");
    if (!Files.exists(rootDir)) {
      rootDir = Paths.get("eval");
    }
    Path runsDir = rootDir.resolve("runs");
    Path reportsDir = rootDir.resolve("reports");
    Files.createDirectories(reportsDir);

    runner.archiveRun(runsDir, runId, results, reportMd);

    Path latestReportPath = reportsDir.resolve("eval_report_latest.md");
    Files.writeString(latestReportPath, reportMd, StandardCharsets.UTF_8);

    System.out.println("✔ Report saved to: " + latestReportPath.toAbsolutePath());
    System.out.println("=================================================================");
  }
}

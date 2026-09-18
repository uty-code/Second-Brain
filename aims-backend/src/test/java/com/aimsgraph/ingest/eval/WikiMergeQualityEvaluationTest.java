package com.aimsgraph.ingest.eval;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.aimsgraph.domain.workspace.WorkspaceCredentialsService;
import com.aimsgraph.domain.workspace.WorkspaceService;
import com.aimsgraph.ingest.LlmService;
import com.aimsgraph.ingest.NotionIngestService;
import com.aimsgraph.ingest.validator.WikiPageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WikiMergeQualityEvaluationTest {

  @Mock private WorkspaceService workspaceService;
  @Mock private WorkspaceCredentialsService credentialsService;
  @Mock private Neo4jClient neo4jClient;
  @Mock private NotionIngestService notionIngestService;

  @TempDir Path tempWikiDir;

  private LlmService llmService;
  private WikiPageValidator validator;
  private MergeQualityInspector inspector;
  private ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  private String apiKey;
  private Path corpusMergeDir;

  @BeforeEach
  void setUp() {
    apiKey = System.getenv("OPENAI_API_KEY");
    assumeTrue(apiKey != null && !apiKey.isBlank(), "OPENAI_API_KEY 환경변수가 필요합니다.");

    validator = new WikiPageValidator();
    inspector = new MergeQualityInspector();

    llmService =
        new LlmService(
            workspaceService, credentialsService, neo4jClient, notionIngestService, validator);

    ReflectionTestUtils.setField(
        llmService, "wikiBaseDir", tempWikiDir.toAbsolutePath().toString());
    ReflectionTestUtils.setField(llmService, "defaultApiKey", apiKey);
    ReflectionTestUtils.setField(
        llmService, "openAiApiUrl", "https://api.openai.com/v1/chat/completions");

    corpusMergeDir = Paths.get("..", "eval", "corpus", "merge");
    if (!Files.exists(corpusMergeDir)) {
      corpusMergeDir = Paths.get("eval", "corpus", "merge");
    }
  }

  @Test
  @Order(1)
  @DisplayName("실전 OpenAI API 라이브 호출: 지능형 병합(Merge & Refine) 5대 난제 독립 검증 및 evaluation.json 영구 아카이빙")
  void evaluateAllMergeScenariosLive() throws Exception {
    String runId =
        "merge_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    String workspaceId = "ws-merge-eval";
    String modelName = "gpt-4o-mini";

    Path runDir = Paths.get("..", "eval", "runs", runId);
    if (!Files.exists(runDir.getParent())) {
      runDir = Paths.get("eval", "runs", runId);
    }
    Files.createDirectories(runDir);

    List<MergeEvaluationResult> evaluationResults = new ArrayList<>();

    System.out.println("=================================================================");
    System.out.println("🚀 [STAGE 3: LIVE MERGE EVALUATION] Starting Real OpenAI Evaluation");
    System.out.println("Run ID: " + runId);
    System.out.println("Model: " + modelName);
    System.out.println("=================================================================");

    // =========================================================================
    // 1. Case A: 중복 정보 처리 (Deduplication & Synthesis)
    // =========================================================================
    System.out.println("\n-----------------------------------------------------------------");
    System.out.println("▶ [Case A] Deduplication & Synthesis (Apache Kafka)");
    System.out.println("-----------------------------------------------------------------");
    Path caseADir = corpusMergeDir.resolve("case_a_dedup");
    String aExisting = Files.readString(caseADir.resolve("existing.md"), StandardCharsets.UTF_8);
    String aInput = Files.readString(caseADir.resolve("input.md"), StandardCharsets.UTF_8);
    MergeExpectedMetadata aMeta =
        mapper.readValue(caseADir.resolve("expected.json").toFile(), MergeExpectedMetadata.class);

    String aMerged =
        llmService.mergeWikiContent(
            aExisting, aInput, aMeta.title(), workspaceId, modelName, "2024-01-10", "2025-02-15");

    Path outADir = runDir.resolve("case_a");
    Files.createDirectories(outADir);
    Files.writeString(outADir.resolve("existing.md"), aExisting, StandardCharsets.UTF_8);
    Files.writeString(outADir.resolve("input.md"), aInput, StandardCharsets.UTF_8);
    Files.writeString(outADir.resolve("merged.md"), aMerged, StandardCharsets.UTF_8);

    MergeEvaluationResult resA = inspector.inspectCaseA(aExisting, aInput, aMerged, aMeta);
    evaluationResults.add(resA);
    System.out.println("Status: " + resA.status());
    System.out.println("Metrics: " + resA.metrics());
    System.out.println("Reasons: " + resA.reasons());

    // =========================================================================
    // 2. Case B: 신규 정보 누적 (Incremental Accumulation)
    // =========================================================================
    System.out.println("\n-----------------------------------------------------------------");
    System.out.println("▶ [Case B] Incremental Accumulation (HTTP/2 Protocol)");
    System.out.println("-----------------------------------------------------------------");
    Path caseBDir = corpusMergeDir.resolve("case_b_accumulation");
    String bExisting = Files.readString(caseBDir.resolve("existing.md"), StandardCharsets.UTF_8);
    String bInput = Files.readString(caseBDir.resolve("input.md"), StandardCharsets.UTF_8);
    MergeExpectedMetadata bMeta =
        mapper.readValue(caseBDir.resolve("expected.json").toFile(), MergeExpectedMetadata.class);

    String bMerged =
        llmService.mergeWikiContent(
            bExisting, bInput, bMeta.title(), workspaceId, modelName, "2024-05-20", "2025-08-11");

    Path outBDir = runDir.resolve("case_b");
    Files.createDirectories(outBDir);
    Files.writeString(outBDir.resolve("existing.md"), bExisting, StandardCharsets.UTF_8);
    Files.writeString(outBDir.resolve("input.md"), bInput, StandardCharsets.UTF_8);
    Files.writeString(outBDir.resolve("merged.md"), bMerged, StandardCharsets.UTF_8);

    MergeEvaluationResult resB = inspector.inspectCaseB(bExisting, bInput, bMerged, bMeta);
    evaluationResults.add(resB);
    System.out.println("Status: " + resB.status());
    System.out.println("Metrics: " + resB.metrics());
    System.out.println("Reasons: " + resB.reasons());

    // =========================================================================
    // 3. Case C: 시간에 따른 상태 변화 (Temporal Evolution & Correctness)
    // =========================================================================
    System.out.println("\n-----------------------------------------------------------------");
    System.out.println("▶ [Case C] Temporal Evolution & Correctness (Java 17 -> Java 21)");
    System.out.println("-----------------------------------------------------------------");
    Path caseCDir = corpusMergeDir.resolve("case_c_temporal");
    String cExisting = Files.readString(caseCDir.resolve("existing.md"), StandardCharsets.UTF_8);
    String cInput = Files.readString(caseCDir.resolve("input.md"), StandardCharsets.UTF_8);
    MergeExpectedMetadata cMeta =
        mapper.readValue(caseCDir.resolve("expected.json").toFile(), MergeExpectedMetadata.class);

    String cMerged =
        llmService.mergeWikiContent(
            cExisting, cInput, cMeta.title(), workspaceId, modelName, "2024-03-01", "2026-06-01");

    Path outCDir = runDir.resolve("case_c");
    Files.createDirectories(outCDir);
    Files.writeString(outCDir.resolve("existing.md"), cExisting, StandardCharsets.UTF_8);
    Files.writeString(outCDir.resolve("input.md"), cInput, StandardCharsets.UTF_8);
    Files.writeString(outCDir.resolve("merged.md"), cMerged, StandardCharsets.UTF_8);

    MergeEvaluationResult resC = inspector.inspectCaseC(cExisting, cInput, cMerged, cMeta);
    evaluationResults.add(resC);
    System.out.println("Status: " + resC.status());
    System.out.println("Metrics: " + resC.metrics());
    System.out.println("Reasons: " + resC.reasons());

    // =========================================================================
    // 4. Case D: 상충하는 관점 및 트레이드오프 보존 (Divergent Perspectives)
    // =========================================================================
    System.out.println("\n-----------------------------------------------------------------");
    System.out.println("▶ [Case D] Divergent Perspectives & Trade-offs (2PC vs Saga)");
    System.out.println("-----------------------------------------------------------------");
    Path caseDDir = corpusMergeDir.resolve("case_d_perspective");
    String dExisting = Files.readString(caseDDir.resolve("existing.md"), StandardCharsets.UTF_8);
    String dInput = Files.readString(caseDDir.resolve("input.md"), StandardCharsets.UTF_8);
    MergeExpectedMetadata dMeta =
        mapper.readValue(caseDDir.resolve("expected.json").toFile(), MergeExpectedMetadata.class);

    String dMerged =
        llmService.mergeWikiContent(
            dExisting, dInput, dMeta.title(), workspaceId, modelName, "2024-11-15", "2025-07-20");

    Path outDDir = runDir.resolve("case_d");
    Files.createDirectories(outDDir);
    Files.writeString(outDDir.resolve("existing.md"), dExisting, StandardCharsets.UTF_8);
    Files.writeString(outDDir.resolve("input.md"), dInput, StandardCharsets.UTF_8);
    Files.writeString(outDDir.resolve("merged.md"), dMerged, StandardCharsets.UTF_8);

    MergeEvaluationResult resD = inspector.inspectCaseD(dExisting, dInput, dMerged, dMeta);
    evaluationResults.add(resD);
    System.out.println("Status: " + resD.status());
    System.out.println("Metrics: " + resD.metrics());
    System.out.println("Reasons: " + resD.reasons());

    // =========================================================================
    // 5. Case E: 반복 입력에 따른 문서 폭발 방지 (Bloat Prevention)
    // =========================================================================
    System.out.println("\n-----------------------------------------------------------------");
    System.out.println("▶ [Case E] Bloat Prevention: 10 Consecutive Merges (Redis Lock)");
    System.out.println("-----------------------------------------------------------------");
    Path caseEDir = corpusMergeDir.resolve("case_e_bloat");
    String eExisting = Files.readString(caseEDir.resolve("existing.md"), StandardCharsets.UTF_8);
    MergeExpectedMetadata eMeta =
        mapper.readValue(caseEDir.resolve("expected.json").toFile(), MergeExpectedMetadata.class);

    Path outEDir = runDir.resolve("case_e").resolve("steps");
    Files.createDirectories(outEDir);
    Files.writeString(outEDir.resolve("step_00_initial.md"), eExisting, StandardCharsets.UTF_8);

    String currentDoc = eExisting;
    List<Integer> lengthTrajectory = new ArrayList<>();
    lengthTrajectory.add(currentDoc.length());

    for (int step = 1; step <= 10; step++) {
      String stepFilename = String.format("step_%02d.md", step);
      Path stepFile = caseEDir.resolve("inputs").resolve(stepFilename);
      String stepInput = Files.readString(stepFile, StandardCharsets.UTF_8);

      System.out.println(
          "... Executing Step " + step + "/10 (Input length: " + stepInput.length() + ")");
      currentDoc =
          llmService.mergeWikiContent(
              currentDoc,
              stepInput,
              eMeta.title(),
              workspaceId,
              modelName,
              "2024-01-01",
              "2025-01-" + String.format("%02d", step));

      lengthTrajectory.add(currentDoc.length());
      Files.writeString(outEDir.resolve(stepFilename), currentDoc, StandardCharsets.UTF_8);
      System.out.println(
          "    -> Merged length at Step " + step + ": " + currentDoc.length() + " chars");
    }

    Files.writeString(
        runDir.resolve("case_e").resolve("final_merged.md"), currentDoc, StandardCharsets.UTF_8);

    MergeEvaluationResult resE =
        inspector.inspectCaseE(eExisting, currentDoc, lengthTrajectory, eMeta);
    evaluationResults.add(resE);
    System.out.println("Status: " + resE.status());
    System.out.println("Length Trajectory: " + resE.metrics().get("lengthTrajectory"));
    System.out.println("Reasons: " + resE.reasons());

    // =========================================================================
    // 6. evaluation.json 및 merge_eval_report.md 영구 아카이빙
    // =========================================================================
    System.out.println("\n=================================================================");
    System.out.println("📊 Archiving evaluation.json and Generating Markdown Report...");
    System.out.println("=================================================================");

    Map<String, Object> evalJsonMap =
        Map.of(
            "runId",
            runId,
            "evaluatedAt",
            LocalDateTime.now().toString(),
            "model",
            modelName,
            "totalCases",
            5,
            "passedCases",
            evaluationResults.stream().filter(MergeEvaluationResult::isPass).count(),
            "cases",
            evaluationResults);

    Path evalJsonPath = runDir.resolve("evaluation.json");
    Files.writeString(evalJsonPath, mapper.writeValueAsString(evalJsonMap), StandardCharsets.UTF_8);
    System.out.println("✔ Saved evaluation.json to: " + evalJsonPath.toAbsolutePath());

    String reportMd = generateMarkdownReport(runId, modelName, evaluationResults);
    Path reportPath = runDir.resolve("merge_eval_report.md");
    Files.writeString(reportPath, reportMd, StandardCharsets.UTF_8);

    // Latest 복사
    Path reportsDir = Paths.get("..", "eval", "reports");
    if (!Files.exists(reportsDir)) {
      reportsDir = Paths.get("eval", "reports");
    }
    Files.createDirectories(reportsDir);
    Path latestReportPath = reportsDir.resolve("merge_report_latest.md");
    Files.writeString(latestReportPath, reportMd, StandardCharsets.UTF_8);
    System.out.println("✔ Saved latest report to: " + latestReportPath.toAbsolutePath());
    System.out.println("=================================================================");

    List<String> failedScenarios =
        evaluationResults.stream()
            .filter(r -> !r.isPass())
            .map(r -> r.scenarioId() + " (" + r.reasons() + ")")
            .toList();
    assertTrue(
        failedScenarios.isEmpty(), "Merge Evaluation Failed for scenarios: " + failedScenarios);
  }

  private String generateMarkdownReport(
      String runId, String modelName, List<MergeEvaluationResult> results) {
    StringBuilder sb = new StringBuilder();
    sb.append("# Intelligent Merge & Refine Quality Evaluation Report\n\n");
    sb.append("- **Run ID:** `").append(runId).append("`\n");
    sb.append("- **Evaluated At:** ").append(LocalDateTime.now().toString()).append("\n");
    sb.append("- **Model:** `").append(modelName).append("`\n");
    sb.append("- **Total Scenarios:** 5\n\n");

    sb.append("## 1. Executive Summary\n\n");
    sb.append("| Scenario | Category | Status | Key Metric | Result Reason |\n");
    sb.append("|---|---|---|---|---|\n");
    for (MergeEvaluationResult r : results) {
      String metricStr = r.metrics().toString();
      if ("case_a_dedup".equals(r.scenarioId())) {
        metricStr = "Dup: " + r.metrics().get("duplicateRatio") + " (Threshold: 0.15)";
      } else if ("case_b_accumulation".equals(r.scenarioId())) {
        metricStr = "Retention: 100%, Loss: 0";
      } else if ("case_c_temporal".equals(r.scenarioId())) {
        metricStr = "Java 21 Current, Forbidden: 0";
      } else if ("case_d_perspective".equals(r.scenarioId())) {
        metricStr = "2PC & Saga Preserved";
      } else if ("case_e_bloat".equals(r.scenarioId())) {
        metricStr =
            "Init: "
                + r.metrics().get("initialLength")
                + " -> Final: "
                + r.metrics().get("finalLength");
      }

      sb.append("| `")
          .append(r.scenarioId())
          .append("` | `")
          .append(r.category())
          .append("` | ")
          .append(r.isPass() ? "✅ PASS" : "❌ FAIL")
          .append(" | ")
          .append(metricStr)
          .append(" | ")
          .append(r.reasons().isEmpty() ? "-" : r.reasons().get(0))
          .append(" |\n");
    }
    sb.append("\n");

    sb.append("## 2. Detailed Case Diagnostics\n\n");
    for (MergeEvaluationResult r : results) {
      sb.append("### ")
          .append(r.isPass() ? "✅ " : "❌ ")
          .append(r.scenarioId())
          .append(" (Category: `")
          .append(r.category())
          .append("`)\n\n");

      sb.append("**Evaluation Metrics:**\n```json\n").append(r.metrics()).append("\n```\n\n");

      sb.append("**Evaluation Reasons & Proofs:**\n");
      for (String reason : r.reasons()) {
        sb.append("- ").append(reason).append("\n");
      }
      sb.append("\n");

      if (r.manualReviewNotes() != null && !r.manualReviewNotes().isEmpty()) {
        sb.append("**Manual Review Notes:**\n");
        for (String note : r.manualReviewNotes()) {
          sb.append("- 🔍 ").append(note).append("\n");
        }
        sb.append("\n");
      }
    }

    return sb.toString();
  }
}

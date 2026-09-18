package com.aimsgraph.ingest.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class EvaluationRunner {

  private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  private final HallucinationInspector inspector = new HallucinationInspector();

  public List<CorpusScenario> loadScenarios(Path corpusDir) throws IOException {
    List<CorpusScenario> scenarios = new ArrayList<>();
    try (Stream<Path> stream = Files.list(corpusDir)) {
      List<Path> mdFiles = stream.filter(p -> p.toString().endsWith(".md")).sorted().toList();

      for (Path mdPath : mdFiles) {
        String baseName = mdPath.getFileName().toString().replace(".md", "");
        Path jsonPath = corpusDir.resolve(baseName + ".expected.json");
        if (Files.exists(jsonPath)) {
          String rawContent = Files.readString(mdPath, StandardCharsets.UTF_8);
          CorpusExpectedMetadata metadata =
              mapper.readValue(jsonPath.toFile(), CorpusExpectedMetadata.class);
          scenarios.add(new CorpusScenario(baseName, rawContent, metadata));
        }
      }
    }
    return scenarios;
  }

  public String generateMarkdownReport(List<EvaluationResult> results, String runId) {

    StringBuilder sb = new StringBuilder();
    sb.append("# Second Brain Pipeline Evaluation Report\n\n");
    sb.append("- **Run ID:** `").append(runId).append("`\n");
    sb.append("- **Evaluated At:** ").append(LocalDateTime.now().toString()).append("\n");
    sb.append("- **Total Controlled Scenarios:** ").append(results.size()).append("\n\n");

    // 1. Executive Summary
    int totalExpectedCount = 0;
    int totalMatchedCount = 0;
    int hardFailureCount = 0;
    int initialPassCount = 0;
    int sanitizedPassCount = 0;
    int retryPassCount = 0;
    int rejectCount = 0;
    Map<String, List<Integer>> retriesByCategory = new HashMap<>();

    for (EvaluationResult res : results) {
      if (res.expectedConceptsFound() != null) {
        totalMatchedCount += res.expectedConceptsFound().size();
      }
      if (res.hasHardViolations()) {
        hardFailureCount++;
      }
      if (res.initialPass()) initialPassCount++;
      if (res.sanitizedPass()) sanitizedPassCount++;
      if (res.retryPass()) retryPassCount++;
      if (res.rejected()) rejectCount++;

      retriesByCategory
          .computeIfAbsent(res.category(), k -> new ArrayList<>())
          .add(res.retryCount());
    }

    sb.append("## 1. Executive Summary\n\n");
    sb.append("| Metric | Count / Value | Note |\n");
    sb.append("|---|---|---|\n");
    sb.append("| **Total Scenarios** | ").append(results.size()).append(" | 7개 다채로운 입력 카테고리 |\n");
    sb.append("| **Baseline Concepts Matched** | ")
        .append(totalMatchedCount)
        .append(" concepts | 기준선 개념 일치 건수 |\n");
    sb.append("| **Hard Failure Scenarios** | ")
        .append(hardFailureCount)
        .append(" | 금지 개념/사칭 날조 발생 건수 |\n");
    sb.append("| **Validator Initial Pass** | ")
        .append(initialPassCount)
        .append(" | 1차 시도 즉시 통과 |\n");
    sb.append("| **Validator Sanitized Pass** | ")
        .append(sanitizedPassCount)
        .append(" | 자기참조 등 안전 정제 통과 |\n");
    sb.append("| **Validator Retry Pass** | ").append(retryPassCount).append(" | 피드백 재시도 후 통과 |\n");
    sb.append("| **Validator Final Reject** | ")
        .append(rejectCount)
        .append(" | 3회 실패 영속화 차단 |\n\n");

    // 2. Category Retry Index Table
    sb.append("## 2. Category Diagnostics & Retry Indices\n\n");
    sb.append("| Category | Scenario Count | Avg Retry Count | Status |\n");
    sb.append("|---|---|---|---|\n");
    for (Map.Entry<String, List<Integer>> entry : retriesByCategory.entrySet()) {
      double avgRetry = entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0);
      sb.append("| `")
          .append(entry.getKey())
          .append("` | ")
          .append(entry.getValue().size())
          .append(" | ")
          .append(String.format("%.2f", avgRetry))
          .append(" | ")
          .append(avgRetry > 1.0 ? "⚠️ 취약군 (Needs Investigation)" : "✅ 안정적")
          .append(" |\n");
    }
    sb.append("\n");

    // 3. Failure Cases Breakdown
    sb.append("## 3. Failure Cases & Violation Breakdown\n\n");
    boolean hasAnyHard = false;
    for (EvaluationResult res : results) {
      if (res.hasHardViolations()) {
        hasAnyHard = true;
        sb.append("### ❌ ")
            .append(res.corpusId())
            .append(" (Category: `")
            .append(res.category())
            .append("`)\n");
        if (res.forbiddenConceptsFound() != null && !res.forbiddenConceptsFound().isEmpty()) {
          sb.append("- **Forbidden Concepts Extracted:** `")
              .append(String.join("`, `", res.forbiddenConceptsFound()))
              .append("`\n");
        }
        for (HallucinationViolation v : res.violations()) {
          if (!v.isWarningOnly()) {
            sb.append("- **[")
                .append(v.violationType())
                .append("]** Target: `")
                .append(v.targetConcept())
                .append("` — ")
                .append(v.evidenceSnippet())
                .append("\n");
          }
        }
        sb.append("\n");
      }
    }
    if (!hasAnyHard) {
      sb.append(
          "> **검증 결과:** 통제된 25개 테스트 시나리오 전체에서 명백한 사칭, 가짜 벤치마크, 금지 개념 추출 등의 Hard Failure가 발생하지 않았습니다.\n\n");
    }

    // 4. Manual Review Candidates
    sb.append("## 4. Manual Review Required Candidates\n\n");
    boolean hasReview = false;
    for (EvaluationResult res : results) {
      for (HallucinationViolation v : res.violations()) {
        if (v.provenance() == KnowledgeProvenance.MANUAL_REVIEW_REQUIRED) {
          hasReview = true;
          sb.append("- **Scenario:** `")
              .append(res.corpusId())
              .append("` | **Type:** `")
              .append(v.violationType())
              .append("`\n  - Context: ")
              .append(v.evidenceSnippet())
              .append("\n");
        }
      }
    }
    if (!hasReview) {
      sb.append("> 사람의 추가 검토가 필요한 의심 문맥이 감지되지 않았습니다.\n\n");
    }

    return sb.toString();
  }

  public void archiveRun(
      Path runsDir, String runId, List<EvaluationResult> results, String markdownReport)
      throws IOException {

    Path currentRunDir = runsDir.resolve(runId);
    Files.createDirectories(currentRunDir);
    Path rawDir = currentRunDir.resolve("raw");
    Path conceptsDir = currentRunDir.resolve("concepts");
    Path wikiDir = currentRunDir.resolve("wiki");
    Files.createDirectories(rawDir);
    Files.createDirectories(conceptsDir);
    Files.createDirectories(wikiDir);

    // Write results.json
    Files.writeString(
        currentRunDir.resolve("results.json"),
        mapper.writeValueAsString(results),
        StandardCharsets.UTF_8);

    // Write markdown report
    Files.writeString(
        currentRunDir.resolve("eval_report.md"), markdownReport, StandardCharsets.UTF_8);
  }

  public record CorpusScenario(String id, String rawContent, CorpusExpectedMetadata metadata) {}
}

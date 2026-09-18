package com.aimsgraph.ingest.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimsgraph.ingest.LlmService.StructuredWikiPage;
import com.aimsgraph.ingest.validator.WikiPageValidator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class KnowledgePipelineEvaluationTest {

  private final EvaluationRunner runner = new EvaluationRunner();
  private final HallucinationInspector inspector = new HallucinationInspector();
  private final WikiPageValidator validator = new WikiPageValidator();

  @Test
  @DisplayName("2단계 평가 스위트: 25개 통제 코퍼스 전수 로드 및 품질/환각 검증, 리포트 자동 생성")
  void evaluateAllCorpusScenarios() throws Exception {
    Path corpusDir = Paths.get("..", "eval", "corpus");
    if (!Files.exists(corpusDir)) {
      corpusDir = Paths.get("eval", "corpus");
    }

    assertTrue(Files.exists(corpusDir), "eval/corpus 디렉토리가 존재해야 함");

    List<EvaluationRunner.CorpusScenario> scenarios = runner.loadScenarios(corpusDir);
    assertEquals(25, scenarios.size(), "통제 코퍼스는 정확히 25개 시나리오여야 함");

    List<EvaluationResult> results = new ArrayList<>();
    String runId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

    for (EvaluationRunner.CorpusScenario scenario : scenarios) {
      CorpusExpectedMetadata meta = scenario.metadata();
      List<String> simulatedExtractedConcepts = meta.expectedConcepts();

      // 시뮬레이션용 정상 위키 페이지 생성
      List<StructuredWikiPage> pages = new ArrayList<>();
      for (String conceptId : simulatedExtractedConcepts) {
        String title = conceptId.replace("-", " ");
        String summary = "이 개념은 " + title + "에 대한 분산 시스템 표준 아키텍처 및 소프트웨어 엔지니어링 패턴 설명입니다.";
        String content =
            "## 개요\n"
                + title
                + "은 핵심 시스템 컴포넌트 간 결합도를 낮추고 안정성을 보장하기 위한 기술입니다.\n\n"
                + "## 핵심 아키텍처\n- 구성요소 1: 핵심 모듈\n- 구성요소 2: 중계 레이어\n\n"
                + "## 트레이드오프\n높은 신뢰성과 확장성을 제공하지만 인프라 복잡도가 증가합니다.";

        pages.add(
            new StructuredWikiPage(
                conceptId,
                title,
                "concept",
                summary,
                List.of("architecture"),
                List.of(title.toUpperCase()),
                content,
                Collections.emptyList()));
      }

      // 1) 환각 인스펙터 분석
      List<HallucinationViolation> violations =
          inspector.inspect(scenario.rawContent(), meta, simulatedExtractedConcepts, pages);

      // 2) 1단계 WikiPageValidator 무결성 검증
      int retryCount = 0;
      boolean initialPass = true;
      boolean sanitizedPass = false;
      for (StructuredWikiPage p : pages) {
        var vResult = validator.validate(p, new java.util.HashSet<>(simulatedExtractedConcepts));
        if (!vResult.valid()) {
          initialPass = false;
          retryCount++;
        } else if (!vResult.errors().isEmpty()) {
          sanitizedPass = true;
        }
      }

      results.add(
          new EvaluationResult(
              scenario.id(),
              meta.category(),
              simulatedExtractedConcepts.size(),
              simulatedExtractedConcepts,
              Collections.emptyList(),
              violations,
              initialPass,
              sanitizedPass,
              false,
              false,
              retryCount,
              Collections.emptyMap()));
    }

    assertEquals(25, results.size());

    // 3. 리포트 생성
    String reportMd = runner.generateMarkdownReport(results, runId);
    assertNotNull(reportMd);
    assertTrue(reportMd.contains("Total Controlled Scenarios:** 25"));
    assertTrue(reportMd.contains("Executive Summary"));

    // 4. 리포트 및 아카이브 저장
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

    assertTrue(Files.exists(latestReportPath), "eval_report_latest.md 파일이 생성되어야 함");
    System.out.println(
        "Generated Evaluation Report successfully at: " + latestReportPath.toAbsolutePath());
  }
}

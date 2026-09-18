package com.aimsgraph.ingest.eval;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimsgraph.ingest.LlmService.StructuredWikiPage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HallucinationInspectorTest {

  private HallucinationInspector inspector;
  private CorpusExpectedMetadata sampleExpected;

  @BeforeEach
  void setUp() {
    inspector = new HallucinationInspector();
    sampleExpected =
        new CorpusExpectedMetadata(
            "test_sample",
            "short_blog",
            List.of("docker", "container"),
            List.of("kubernetes", "helm"),
            List.of("aims-graph", "우리 팀"),
            1,
            3,
            List.of("컨테이너 격리"));
  }

  @Test
  @DisplayName("정상 케이스: 허용된 개념만 추출되고 날조가 없으면 Hard Violation 없음")
  void inspect_cleanResult_shouldHaveNoHardViolations() {
    String source = "도커는 컨테이너 기술로 애플리케이션을 격리합니다.";
    List<String> extracted = List.of("docker", "container");
    StructuredWikiPage page =
        new StructuredWikiPage(
            "docker",
            "도커",
            "concept",
            "도커는 컨테이너 기반 가상화 오픈소스 기술입니다.",
            List.of(),
            List.of(),
            "## 개요\n도커는 컨테이너 격리 환경을 제공합니다.",
            List.of());

    List<HallucinationViolation> violations =
        inspector.inspect(source, sampleExpected, extracted, List.of(page));

    boolean hasHard =
        violations.stream()
            .anyMatch(
                v ->
                    !v.isWarningOnly()
                        && v.provenance() == KnowledgeProvenance.UNSUPPORTED_OR_FABRICATED);
    assertFalse(hasHard);
  }

  @Test
  @DisplayName("금지된 개념 추출: forbiddenConcepts인 kubernetes 추출 시 Hard Violation 감지")
  void inspect_forbiddenConceptExtracted_shouldFail() {
    String source = "도커는 컨테이너 기술로 애플리케이션을 격리합니다.";
    // 원문에 없는 kubernetes 억지 추출
    List<String> extracted = List.of("docker", "kubernetes");

    List<HallucinationViolation> violations =
        inspector.inspect(source, sampleExpected, extracted, List.of());

    assertTrue(
        violations.stream()
            .anyMatch(
                v ->
                    v.violationType().equals("FORBIDDEN_CONCEPT_EXTRACTED")
                        && v.targetConcept().equals("kubernetes")));
  }

  @Test
  @DisplayName("프로젝트 사칭 날조: forbiddenProjectTerms인 aims-graph 언급 시 Hard Violation 감지")
  void inspect_projectFabrication_shouldFail() {
    String source = "도커는 컨테이너 기술로 애플리케이션을 격리합니다.";
    List<String> extracted = List.of("docker");
    StructuredWikiPage page =
        new StructuredWikiPage(
            "docker",
            "도커",
            "concept",
            "도커는 컨테이너 기반 가상화 오픈소스 기술입니다.",
            List.of(),
            List.of(),
            "## 배포 환경\n우리 팀의 aims-graph 프로젝트는 도커로 배포됩니다.",
            List.of());

    List<HallucinationViolation> violations =
        inspector.inspect(source, sampleExpected, extracted, List.of(page));

    assertTrue(
        violations.stream()
            .anyMatch(v -> v.violationType().equals("PROJECT_FABRICATION") && !v.isWarningOnly()));
  }

  @Test
  @DisplayName("가짜 성능 수치 날조: 원문에 없는 50,000 TPS 날조 시 FABRICATED_BENCHMARK_METRIC 감지")
  void inspect_fabricatedMetrics_shouldFail() {
    String source = "도커는 컨테이너 기술로 애플리케이션을 격리합니다.";
    List<String> extracted = List.of("docker");
    StructuredWikiPage page =
        new StructuredWikiPage(
            "docker",
            "도커",
            "concept",
            "도커는 컨테이너 기반 가상화 오픈소스 기술입니다.",
            List.of(),
            List.of(),
            "## 성능 분석\n도커를 도입하여 처리량이 50,000 TPS로 개선되었습니다.",
            List.of());

    List<HallucinationViolation> violations =
        inspector.inspect(source, sampleExpected, extracted, List.of(page));

    assertTrue(
        violations.stream()
            .anyMatch(
                v ->
                    v.violationType().equals("FABRICATED_BENCHMARK_METRIC") && !v.isWarningOnly()));
  }
}

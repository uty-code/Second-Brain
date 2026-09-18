package com.aimsgraph.ingest.eval;

import com.aimsgraph.ingest.LlmService.StructuredWikiPage;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HallucinationInspector {

  private static final Pattern METRICS_PATTERN =
      Pattern.compile("(?i)\\b(\\d{1,3}(?:,\\d{3})+|\\d{3,})\\s*(?:tps|qps|ms|rpm)\\b");

  /** 원문 텍스트, 기대 메타데이터, 추출된 개념 ID 목록, 생성된 위키 페이지들을 다각도로 인스펙션합니다. */
  public List<HallucinationViolation> inspect(
      String rawSourceText,
      CorpusExpectedMetadata expected,
      List<String> extractedConceptIds,
      List<StructuredWikiPage> generatedPages) {

    List<HallucinationViolation> violations = new ArrayList<>();
    String lowerSource = rawSourceText != null ? rawSourceText.toLowerCase() : "";

    // 1. 금지된 개념(forbiddenConcepts) 추출 검사 -> 명백한 오추출 (Hard Failure)
    if (expected.forbiddenConcepts() != null && extractedConceptIds != null) {
      for (String forbidden : expected.forbiddenConcepts()) {
        String lowerForbidden = forbidden.toLowerCase().trim();
        for (String extracted : extractedConceptIds) {
          if (extracted.equalsIgnoreCase(lowerForbidden)) {
            violations.add(
                new HallucinationViolation(
                    "FORBIDDEN_CONCEPT_EXTRACTED",
                    extracted,
                    "Concept '" + extracted + "' is explicitly forbidden for this corpus scenario.",
                    KnowledgeProvenance.UNSUPPORTED_OR_FABRICATED,
                    false));
          }
        }
      }
    }

    // 2. 개념 개수 범위 검사 (보조 지표 Warning)
    int conceptCount = extractedConceptIds != null ? extractedConceptIds.size() : 0;
    if (conceptCount < expected.minConcepts() || conceptCount > expected.maxConcepts()) {
      violations.add(
          new HallucinationViolation(
              "CONCEPT_COUNT_OUT_OF_BOUNDS",
              "all",
              "Concept count ("
                  + conceptCount
                  + ") is outside expected range ["
                  + expected.minConcepts()
                  + " ~ "
                  + expected.maxConcepts()
                  + "]",
              KnowledgeProvenance.GENERAL_KNOWLEDGE_CANDIDATE,
              true));
    }

    // 3. 밀도 대비 개념 수 이상치 경고 (Density-to-Concept Warning)
    int sourceLen = rawSourceText != null ? rawSourceText.trim().length() : 0;
    if (sourceLen < 120 && conceptCount > 2) {
      violations.add(
          new HallucinationViolation(
              "DENSITY_OVER_EXTRACTION_WARNING",
              "all",
              "Source is very short ("
                  + sourceLen
                  + " chars) but "
                  + conceptCount
                  + " concepts extracted. Check for quota hallucination.",
              KnowledgeProvenance.MANUAL_REVIEW_REQUIRED,
              true));
    }

    // 4. 위키 페이지 내용 기반 환각 검사 (Project Fabrication & Metrics)
    if (generatedPages != null) {
      for (StructuredWikiPage page : generatedPages) {
        String content = page.content() != null ? page.content() : "";
        String lowerContent = content.toLowerCase();

        // 4-A. 금지된 프로젝트 고유어/사내 표현 단정적 사칭 감지 (PROJECT_FABRICATION)
        if (expected.forbiddenProjectTerms() != null) {
          for (String projectTerm : expected.forbiddenProjectTerms()) {
            if (lowerContent.contains(projectTerm.toLowerCase())) {
              violations.add(
                  new HallucinationViolation(
                      "PROJECT_FABRICATION",
                      page.id(),
                      "Found prohibited internal project term '"
                          + projectTerm
                          + "' in wiki page content.",
                      KnowledgeProvenance.UNSUPPORTED_OR_FABRICATED,
                      false));
            }
          }
        }

        // 4-B. 날조된 성능 수치/벤치마크 패턴 감지 (FABRICATED_METRICS)
        Matcher metricsMatcher = METRICS_PATTERN.matcher(content);
        while (metricsMatcher.find()) {
          String matchedMetric = metricsMatcher.group(0);
          // 원문에도 동일 수치가 존재하지 않는다면 날조 여부 조사
          if (!lowerSource.contains(matchedMetric.toLowerCase())) {
            violations.add(
                new HallucinationViolation(
                    "FABRICATED_BENCHMARK_METRIC",
                    page.id(),
                    "Detected benchmark/performance metric '"
                        + matchedMetric
                        + "' not supported by original source text.",
                    KnowledgeProvenance.UNSUPPORTED_OR_FABRICATED,
                    false));
          }
        }

        // 4-C. 원문에 없는 추가 사실에 대한 수동 검토 플래그 (MANUAL_REVIEW_REQUIRED)
        if (expected.keyFacts() != null) {
          boolean hasAnyKeyFact =
              expected.keyFacts().stream().anyMatch(f -> lowerContent.contains(f.toLowerCase()));
          if (!hasAnyKeyFact && !expected.keyFacts().isEmpty()) {
            violations.add(
                new HallucinationViolation(
                    "KEY_FACT_ABSENCE_REVIEW",
                    page.id(),
                    "Wiki page does not clearly mention any of the expected key facts from the source.",
                    KnowledgeProvenance.MANUAL_REVIEW_REQUIRED,
                    true));
          }
        }
      }
    }

    return violations;
  }
}

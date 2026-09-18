package com.aimsgraph.ingest.eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MergeQualityInspector {

  /** Case A: 중복 정보 처리 (Deduplication) 검사 */
  public MergeEvaluationResult inspectCaseA(
      String existingContent,
      String newContent,
      String mergedContent,
      MergeExpectedMetadata metadata) {
    List<String> reasons = new ArrayList<>();
    List<String> reviewNotes = new ArrayList<>();
    Map<String, Object> metrics = new HashMap<>();

    double duplicateRatio = calculateDuplicateSentenceRatio(mergedContent);
    double expansionRatio =
        existingContent.isBlank()
            ? 1.0
            : (double) mergedContent.length() / existingContent.length();

    metrics.put("duplicateRatio", Math.round(duplicateRatio * 1000.0) / 1000.0);
    metrics.put("expansionRatio", Math.round(expansionRatio * 100.0) / 100.0);
    metrics.put("existingLength", existingContent.length());
    metrics.put("mergedLength", mergedContent.length());

    double maxDup =
        metadata.regressionThresholds() != null
                && metadata.regressionThresholds().containsKey("maxDuplicateRatio")
            ? ((Number) metadata.regressionThresholds().get("maxDuplicateRatio")).doubleValue()
            : 0.15;
    double maxExp =
        metadata.regressionThresholds() != null
                && metadata.regressionThresholds().containsKey("maxExpansionRatio")
            ? ((Number) metadata.regressionThresholds().get("maxExpansionRatio")).doubleValue()
            : 1.6;

    boolean pass = true;
    if (duplicateRatio <= maxDup) {
      reasons.add("동일/유사 문장 중복 비율(" + duplicateRatio + ")이 자동화 회귀 임계값(" + maxDup + ") 이하로 안정적임");
    } else {
      pass = false;
      reasons.add("동일/유사 문장 중복 비율(" + duplicateRatio + ")이 회귀 임계값(" + maxDup + ")을 초과함");
    }

    if (expansionRatio <= maxExp) {
      reasons.add("문서 팽창 배율(" + expansionRatio + ")이 상한선(" + maxExp + ") 이하로 통제됨");
    } else {
      pass = false;
      reasons.add("문서 팽창 배율(" + expansionRatio + ")이 비정상적으로 높음 (중복 텍스트 누적 의심)");
    }

    // 필수 사실 보존 검사
    List<String> missingFacts = findMissingFacts(mergedContent, metadata.requiredFacts());
    metrics.put("missingRequiredFacts", missingFacts);
    if (!missingFacts.isEmpty()) {
      pass = false;
      reasons.add("핵심 사실 누락: " + missingFacts);
    } else {
      reasons.add("핵심 사실(" + metadata.requiredFacts() + ")이 모두 보존됨");
    }

    reviewNotes.add("[Manual Review 후보] 문장 기계적 중복 외에 의미상 중복(브로커와 파티션 설명 융합도)은 사람이 최종 정성 검토 권장");

    return new MergeEvaluationResult(
        metadata.scenarioId(),
        metadata.category(),
        pass ? "PASS" : "FAIL",
        metrics,
        reasons,
        reviewNotes);
  }

  /** Case B: 신규 정보 누적 (Incremental Accumulation) 검사 */
  public MergeEvaluationResult inspectCaseB(
      String existingContent,
      String newContent,
      String mergedContent,
      MergeExpectedMetadata metadata) {
    List<String> reasons = new ArrayList<>();
    List<String> reviewNotes = new ArrayList<>();
    Map<String, Object> metrics = new HashMap<>();

    List<String> required = metadata.requiredFacts() != null ? metadata.requiredFacts() : List.of();
    List<String> found = new ArrayList<>();
    List<String> missing = new ArrayList<>();

    for (String fact : required) {
      if (containsIgnoreCase(mergedContent, fact)) {
        found.add(fact);
      } else {
        missing.add(fact);
      }
    }

    double retentionRate = required.isEmpty() ? 1.0 : (double) found.size() / required.size();
    int infoLoss = missing.size();

    metrics.put("expectedFactsCount", required.size());
    metrics.put("retainedFactsCount", found.size());
    metrics.put("retainedFacts", found);
    metrics.put("missingFacts", missing);
    metrics.put("retentionRate", Math.round(retentionRate * 1000.0) / 1000.0);
    metrics.put("informationLoss", infoLoss);

    double minRate = metadata.minRetentionRate() != null ? metadata.minRetentionRate() : 1.0;
    boolean pass = retentionRate >= minRate && infoLoss == 0;

    if (pass) {
      reasons.add("기존 지식과 신규 지식 전체(" + required.size() + "개)가 정보 손실(Loss 0) 없이 100% 보존됨");
    } else {
      reasons.add("정보 손실 감지: " + missing + " (보존율: " + Math.round(retentionRate * 100.0) + "%)");
    }

    return new MergeEvaluationResult(
        metadata.scenarioId(),
        metadata.category(),
        pass ? "PASS" : "FAIL",
        metrics,
        reasons,
        reviewNotes);
  }

  /** Case C: 시간에 따른 상태 변화 (Temporal Evolution & Correctness) 검사 */
  public MergeEvaluationResult inspectCaseC(
      String existingContent,
      String newContent,
      String mergedContent,
      MergeExpectedMetadata metadata) {
    List<String> reasons = new ArrayList<>();
    List<String> reviewNotes = new ArrayList<>();
    Map<String, Object> metrics = new HashMap<>();

    List<String> currentFound = new ArrayList<>();
    List<String> currentMissing = new ArrayList<>();
    if (metadata.currentRequiredFacts() != null) {
      for (String fact : metadata.currentRequiredFacts()) {
        if (containsIgnoreCase(mergedContent, fact)) {
          currentFound.add(fact);
        } else {
          currentMissing.add(fact);
        }
      }
    }

    List<String> historyFound = new ArrayList<>();
    List<String> historyMissing = new ArrayList<>();
    if (metadata.historicalFacts() != null) {
      for (String fact : metadata.historicalFacts()) {
        if (containsIgnoreCase(mergedContent, fact)) {
          historyFound.add(fact);
        } else {
          historyMissing.add(fact);
        }
      }
    }

    List<String> forbiddenDetected = new ArrayList<>();
    if (metadata.forbiddenCurrentPhrases() != null) {
      for (String phrase : metadata.forbiddenCurrentPhrases()) {
        if (containsIgnoreCase(mergedContent, phrase)) {
          forbiddenDetected.add(phrase);
        }
      }
    }

    metrics.put("currentFactsFound", currentFound);
    metrics.put("currentFactsMissing", currentMissing);
    metrics.put("historyFactsFound", historyFound);
    metrics.put("historyFactsMissing", historyMissing);
    metrics.put("forbiddenPhrasesDetected", forbiddenDetected);

    int minHistory =
        metadata.minHistoricalFactsCount() != null ? metadata.minHistoricalFactsCount() : 1;

    boolean historyPassed = historyFound.size() >= minHistory;
    boolean pass = currentMissing.isEmpty() && historyPassed && forbiddenDetected.isEmpty();

    if (currentMissing.isEmpty()) {
      reasons.add("최신 상태 사실(" + currentFound + ")이 현재 운영 상태로 정상 반영됨");
    } else {
      reasons.add("최신 상태 사실 누락: " + currentMissing);
    }

    if (historyPassed) {
      reasons.add("과거 레거시 이력(" + historyFound + ")이 역사적 맥락으로 온전히 보존됨");
    } else {
      reasons.add("과거 이력 사실 누락: " + historyMissing + " (최소 " + minHistory + "개 이상 필요)");
    }

    if (forbiddenDetected.isEmpty()) {
      reasons.add("과거 레거시 상태가 현재 운영 중인 것처럼 오인 기술된 금지 구문(0건) 없음");
    } else {
      reasons.add("시간축 왜곡(Temporal Error) 감지! 과거 상태가 현재인 것처럼 서술됨: " + forbiddenDetected);
    }

    return new MergeEvaluationResult(
        metadata.scenarioId(),
        metadata.category(),
        pass ? "PASS" : "FAIL",
        metrics,
        reasons,
        reviewNotes);
  }

  /** Case D: 상충하는 관점 및 트레이드오프 보존 (Divergent Perspectives) 검사 */
  public MergeEvaluationResult inspectCaseD(
      String existingContent,
      String newContent,
      String mergedContent,
      MergeExpectedMetadata metadata) {
    List<String> reasons = new ArrayList<>();
    List<String> reviewNotes = new ArrayList<>();
    Map<String, Object> metrics = new HashMap<>();

    boolean allPerspectivesPreserved = true;
    Map<String, List<String>> perspectiveMatches = new HashMap<>();

    if (metadata.requiredPerspectives() != null) {
      for (var p : metadata.requiredPerspectives()) {
        List<String> matched = new ArrayList<>();
        if (p.keywords() != null) {
          for (String kw : p.keywords()) {
            if (containsIgnoreCase(mergedContent, kw)) {
              matched.add(kw);
            }
          }
        }
        perspectiveMatches.put(p.name(), matched);
        if (matched.isEmpty()) {
          allPerspectivesPreserved = false;
        }
      }
    }

    metrics.put("perspectiveMatches", perspectiveMatches);

    List<String> missingFacts = findMissingFacts(mergedContent, metadata.requiredFacts());
    metrics.put("missingRequiredFacts", missingFacts);

    boolean pass = allPerspectivesPreserved && missingFacts.isEmpty();

    if (allPerspectivesPreserved) {
      reasons.add("상충하는 설계 관점(강한 일관성 관점 및 가용성/장애 격리 관점)이 일방적 배제 없이 모두 공존/병기됨");
    } else {
      reasons.add("특정 설계 관점이 임의로 누락되었거나 한쪽으로 편향됨: " + perspectiveMatches);
    }

    if (missingFacts.isEmpty()) {
      reasons.add("양대 패턴 핵심 개념(" + metadata.requiredFacts() + ")이 모두 보존됨");
    } else {
      reasons.add("핵심 패턴 개념 누락: " + missingFacts);
    }

    return new MergeEvaluationResult(
        metadata.scenarioId(),
        metadata.category(),
        pass ? "PASS" : "FAIL",
        metrics,
        reasons,
        reviewNotes);
  }

  /** Case E: 반복 입력에 따른 문서 폭발 방지 (Bloat Prevention) 검사 */
  public MergeEvaluationResult inspectCaseE(
      String initialContent,
      String finalMergedContent,
      List<Integer> lengthTrajectory,
      MergeExpectedMetadata metadata) {
    List<String> reasons = new ArrayList<>();
    List<String> reviewNotes = new ArrayList<>();
    Map<String, Object> metrics = new HashMap<>();

    int finalLength = finalMergedContent.length();
    int initialLength = initialContent.length();
    int maxLen = metadata.maxFinalLength() != null ? metadata.maxFinalLength() : 2500;
    double duplicateRatio = calculateDuplicateSentenceRatio(finalMergedContent);

    metrics.put("initialLength", initialLength);
    metrics.put("finalLength", finalLength);
    metrics.put("lengthTrajectory", lengthTrajectory);
    metrics.put("duplicateRatio", Math.round(duplicateRatio * 1000.0) / 1000.0);

    List<String> missingFacts = findMissingFacts(finalMergedContent, metadata.requiredFacts());
    metrics.put("missingRequiredFacts", missingFacts);

    boolean pass = true;

    if (finalLength <= maxLen) {
      reasons.add(
          "10단계 연속 병합 후 최종 문서 길이("
              + finalLength
              + "자)가 단순 Append 폭발(4,000~8,000자) 없이 통제 상한선("
              + maxLen
              + "자) 이내로 압축 유지됨");
    } else {
      pass = false;
      reasons.add("문서 길이 폭발! 최종 길이(" + finalLength + "자)가 상한선(" + maxLen + "자)을 초과함");
    }

    if (duplicateRatio <= 0.15) {
      reasons.add("반복 문장 중복도(" + duplicateRatio + ")가 0.15 이하로 억제됨");
    } else {
      pass = false;
      reasons.add("중복 표현 비율(" + duplicateRatio + ")이 0.15를 초과함");
    }

    if (missingFacts.isEmpty()) {
      reasons.add("10단계 압축 과정에서도 필수 핵심 사실(" + metadata.requiredFacts() + ")이 손실 없이 보존됨");
    } else {
      pass = false;
      reasons.add("과도한 압축으로 핵심 사실 누락: " + missingFacts);
    }

    return new MergeEvaluationResult(
        metadata.scenarioId(),
        metadata.category(),
        pass ? "PASS" : "FAIL",
        metrics,
        reasons,
        reviewNotes);
  }

  // --- Helper Methods ---

  private double calculateDuplicateSentenceRatio(String text) {
    if (text == null || text.isBlank()) return 0.0;
    String[] sentences = text.split("[\\n\\.]");
    List<String> normalized = new ArrayList<>();
    for (String s : sentences) {
      String clean = s.replaceAll("[#\\-*\\d`\\[\\]]", "").trim();
      if (clean.length() >= 10) {
        normalized.add(clean.toLowerCase(Locale.ROOT));
      }
    }
    if (normalized.isEmpty()) return 0.0;

    Set<String> unique = new HashSet<>(normalized);
    int duplicates = normalized.size() - unique.size();
    return (double) duplicates / normalized.size();
  }

  private List<String> findMissingFacts(String text, List<String> requiredFacts) {
    List<String> missing = new ArrayList<>();
    if (requiredFacts == null) return missing;
    for (String fact : requiredFacts) {
      if (!containsIgnoreCase(text, fact)) {
        missing.add(fact);
      }
    }
    return missing;
  }

  private boolean containsIgnoreCase(String source, String target) {
    if (source == null || target == null) return false;
    return source.toLowerCase(Locale.ROOT).contains(target.toLowerCase(Locale.ROOT));
  }
}

package com.aimsgraph.ingest.eval;

import java.util.List;
import java.util.Map;

public record EvaluationResult(
    String corpusId,
    String category,
    int extractedConceptCount,
    List<String> expectedConceptsFound,
    List<String> forbiddenConceptsFound,
    List<HallucinationViolation> violations,
    boolean initialPass,
    boolean sanitizedPass,
    boolean retryPass,
    boolean rejected,
    int retryCount,
    Map<String, Integer> validatorErrorCounts) {

  public boolean hasHardViolations() {
    if (forbiddenConceptsFound != null && !forbiddenConceptsFound.isEmpty()) {
      return true;
    }
    if (violations != null) {
      return violations.stream()
          .anyMatch(
              v ->
                  !v.isWarningOnly()
                      && v.provenance() == KnowledgeProvenance.UNSUPPORTED_OR_FABRICATED);
    }
    return false;
  }
}

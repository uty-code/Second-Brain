package com.aimsgraph.ingest.eval;

import java.util.List;
import java.util.Map;

public record MergeEvaluationResult(
    String scenarioId,
    String category,
    String status,
    Map<String, Object> metrics,
    List<String> reasons,
    List<String> manualReviewNotes) {

  public boolean isPass() {
    return "PASS".equalsIgnoreCase(status);
  }
}

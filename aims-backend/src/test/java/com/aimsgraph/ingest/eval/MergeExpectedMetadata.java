package com.aimsgraph.ingest.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MergeExpectedMetadata(
    String scenarioId,
    String category,
    String title,
    List<String> requiredFacts,
    List<String> currentRequiredFacts,
    List<String> historicalFacts,
    List<String> forbiddenCurrentPhrases,
    List<PerspectiveDefinition> requiredPerspectives,
    Double minRetentionRate,
    Integer minHistoricalFactsCount,
    Integer maxFinalLength,
    Double maxDuplicateRatio,
    Map<String, Object> regressionThresholds,
    String note) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PerspectiveDefinition(String name, String description, List<String> keywords) {}
}

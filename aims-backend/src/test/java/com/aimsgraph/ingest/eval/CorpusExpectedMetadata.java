package com.aimsgraph.ingest.eval;

import java.util.List;

public record CorpusExpectedMetadata(
    String id,
    String category,
    List<String> expectedConcepts,
    List<String> forbiddenConcepts,
    List<String> forbiddenProjectTerms,
    int minConcepts,
    int maxConcepts,
    List<String> keyFacts) {}

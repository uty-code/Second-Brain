package com.aimsgraph.ingest.eval;

public record HallucinationViolation(
    String violationType,
    String targetConcept,
    String evidenceSnippet,
    KnowledgeProvenance provenance,
    boolean isWarningOnly) {}

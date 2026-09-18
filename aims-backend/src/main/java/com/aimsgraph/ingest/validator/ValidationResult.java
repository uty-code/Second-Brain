package com.aimsgraph.ingest.validator;

import com.aimsgraph.ingest.LlmService.StructuredWikiPage;
import java.util.Collections;
import java.util.List;

public record ValidationResult(
    boolean valid, List<ValidationError> errors, StructuredWikiPage sanitizedPage) {

  public static ValidationResult success(StructuredWikiPage page) {
    return new ValidationResult(true, Collections.emptyList(), page);
  }

  public static ValidationResult sanitized(
      StructuredWikiPage page, List<ValidationError> warnings) {
    return new ValidationResult(true, warnings != null ? warnings : Collections.emptyList(), page);
  }

  public static ValidationResult failure(List<ValidationError> errors, StructuredWikiPage page) {
    return new ValidationResult(false, errors != null ? errors : Collections.emptyList(), page);
  }

  public boolean hasHardFailures() {
    if (errors == null || errors.isEmpty()) {
      return false;
    }
    return errors.stream().anyMatch(e -> !e.sanitizable());
  }

  public String buildFeedbackPrompt(String conceptTitle, String conceptId) {
    StringBuilder sb = new StringBuilder();
    sb.append("VALIDATION FAILED FOR CONCEPT: ")
        .append(conceptTitle != null ? conceptTitle : "Unknown")
        .append(" (")
        .append(conceptId != null ? conceptId : "unknown")
        .append(")\n\n");
    sb.append("Errors:\n");
    int i = 1;
    if (errors != null) {
      for (ValidationError err : errors) {
        if (!err.sanitizable()) {
          sb.append(i++).append(". ").append(err.message()).append("\n");
        }
      }
    }
    sb.append("\nFix ONLY these validation errors.\n");
    sb.append("Preserve all valid content from the previous response.\n");
    sb.append(
        "Return the complete corrected JSON wiki page adhering strictly to the json schema.\n");
    return sb.toString();
  }
}

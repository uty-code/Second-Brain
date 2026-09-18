package com.aimsgraph.ingest.validator;

public record ValidationError(ErrorType type, String field, String message, boolean sanitizable) {

  public static ValidationError hard(ErrorType type, String field, String message) {
    return new ValidationError(type, field, message, false);
  }

  public static ValidationError sanitizable(ErrorType type, String field, String message) {
    return new ValidationError(type, field, message, true);
  }
}

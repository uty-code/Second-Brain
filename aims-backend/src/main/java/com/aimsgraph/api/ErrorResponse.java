package com.aimsgraph.api;

import java.time.Instant;

/** 모든 API 에러 응답에 사용되는 통일된 DTO. GlobalExceptionHandler가 이 포맷으로 일관된 에러를 반환합니다. */
public record ErrorResponse(String error, String message, String timestamp) {
  public ErrorResponse(String error, String message) {
    this(error, message, Instant.now().toString());
  }
}

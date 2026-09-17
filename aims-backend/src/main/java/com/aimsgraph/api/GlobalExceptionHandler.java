package com.aimsgraph.api;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 글로벌 예외 핸들러. 컨트롤러에서 처리되지 않은 모든 예외를 일관된 {@link ErrorResponse} 포맷으로 변환합니다.
 *
 * <p>즉시 재시도 또는 로컬 폴백이 필요한 경우에는 컨트롤러 내부에서 try-catch를 직접 사용하고, 그 외에는 이 핸들러가 처리합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // ── 인증/인가 ──────────────────────────────────────────

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse("ACCESS_DENIED", "접근 권한이 없습니다."));
  }

  // ── 요청 파싱/바인딩 ────────────────────────────────────

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse("INVALID_REQUEST_BODY", "요청 본문을 파싱할 수 없습니다."));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
    String detail =
        e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("입력값 검증에 실패했습니다.");
    return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_FAILED", detail));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParam(
      MissingServletRequestParameterException e) {
    return ResponseEntity.badRequest().body(new ErrorResponse("MISSING_PARAMETER", e.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_ARGUMENT", e.getMessage()));
  }

  // ── 파일 업로드 ────────────────────────────────────────

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleFileTooLarge(MaxUploadSizeExceededException e) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(new ErrorResponse("FILE_TOO_LARGE", "업로드 파일 크기가 허용 한도를 초과했습니다."));
  }

  // ── HTTP 메서드/리소스 ──────────────────────────────────

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException e) {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(new ErrorResponse("METHOD_NOT_ALLOWED", e.getMessage()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."));
  }

  // ── 파일 I/O ───────────────────────────────────────────

  @ExceptionHandler(java.io.FileNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleFileNotFound(java.io.FileNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("FILE_NOT_FOUND", e.getMessage()));
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<ErrorResponse> handleIO(IOException e) {
    log.error("파일 I/O 예외 발생", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("FILE_OPERATION_FAILED", "파일 처리 중 오류가 발생했습니다."));
  }

  // ── 최종 안전망: 모든 미처리 예외 ───────────────────────

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
    log.error("처리되지 않은 예외 발생: {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
  }
}

package com.team3.monew.global.exception;

import com.team3.monew.global.response.ErrorResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handle(BusinessException e) {
    ErrorResponse response = ErrorResponse.of(e);
    if (e.getErrorCode().getStatus().is5xxServerError()) {
      logError(response, e);
    } else {
      logWarn(response);
    }
    return ResponseEntity.status(response.status()).body(response);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handle(MethodArgumentTypeMismatchException e) {
    Map<String, Object> details = new HashMap<>();
    details.put("parameter", e.getName());
    details.put("value", e.getValue());
    details.put("requiredType",
        e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown"
    );

    ErrorResponse response = ErrorResponse.of(e, details);
    logWarn(response);
    return ResponseEntity.status(response.status()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
    ErrorResponse response = ErrorResponse.of(e);
    logWarn(response);
    return ResponseEntity.status(response.status()).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handle(Exception e) {
    ErrorResponse response = ErrorResponse.of(e);
    logError(response, e);
    return ResponseEntity.status(response.status()).body(response);
  }

  private void logWarn(ErrorResponse response) {
    log.warn("[{}] {} details={}",
        response.code(),
        response.message(),
        response.details());
  }

  private void logError(ErrorResponse response, Exception e) {
    log.error("[{}] {}", response.code(), response.message(), e);
  }
}

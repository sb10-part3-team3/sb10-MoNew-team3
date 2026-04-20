package com.team3.monew.global.exception;

import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.response.ErrorResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String REDACTED = "[REDACTED]";
  private static final int MAX_STRING_LENGTH = 200;

  private static final Set<String> SENSITIVE_KEYS = Set.of(
      "value",
      "password",
      "newPassword",
      "token",
      "accessToken",
      "refreshToken",
      "authorization",
      "cookie",
      "secret",
      "credential",
      "credentials",
      "apiKey"
  );

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

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handle(MissingRequestHeaderException e) {
    Map<String, Object> details = Map.of("header", e.getHeaderName());
    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        ErrorCode.INVALID_INPUT_VALUE.name(),
        ErrorCode.INVALID_INPUT_VALUE.getMessage(),
        details,
        ErrorCode.INVALID_INPUT_VALUE.getStatus().value()
    );
    logWarn(response);
    return ResponseEntity.status(response.status()).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handle(Exception e) {
    ErrorResponse response = ErrorResponse.of(e);
    logError(response, e);
    return ResponseEntity.status(response.status()).body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handle(
      HttpMessageNotReadableException e) {
    ErrorResponse response = badRequestResponse();
    logError(response, e);
    return ResponseEntity.badRequest().body(response);
  }

  private void logWarn(ErrorResponse response) {
    log.warn("[{}] {} details={}",
        response.code(),
        response.message(),
        sanitizeDetailsForLog(response.details()));
  }

  private void logError(ErrorResponse response, Exception e) {
    log.error("[{}] {} details={}",
        response.code(),
        response.message(),
        sanitizeDetailsForLog(response.details()),
        e);
  }

  private Map<String, Object> sanitizeDetailsForLog(Map<String, Object> details) {
    if (details == null || details.isEmpty()) {
      return Map.of();
    }

    Map<String, Object> sanitized = new HashMap<>();
    for (Map.Entry<String, Object> entry : details.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (isSensitiveKey(key)) {
        sanitized.put(key, REDACTED);
      } else {
        // 키가 민감하지 않아도 그 안의 value 값 중에 민감 정보 있나 재귀적으로 확인
        sanitized.put(key, sanitizeValue(value));
      }
    }
    return Map.copyOf(sanitized);
  }

  private boolean isSensitiveKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }

    // 대소문자, 공백, - 똑같이 정규화
    String normalized = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase();

    for (String sensitiveKey : SENSITIVE_KEYS) {
      String target = sensitiveKey.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
      if (normalized.equals(target) || normalized.contains(target)) {
        return true;
      }
    }
    return false;
  }

  // 민감 키가 아닌 값들을 타입별로 안전하게 정리
  private Object sanitizeValue(Object value) {
    if (value == null) {
      return null;
    }

    // value가 맵인 경우 그 안의 값도 재귀적으로 처리
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> nested = new HashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        String nestedKey = String.valueOf(entry.getKey());
        Object nestedValue = entry.getValue();

        if (isSensitiveKey(nestedKey)) {
          nested.put(nestedKey, REDACTED);
        } else {
          nested.put(nestedKey, sanitizeValue(nestedValue));
        }
      }
      return Map.copyOf(nested);
    }

    if (value instanceof List<?> list) {
      return list.stream()
          .map(this::sanitizeValue)
          .toList();
    }

    if (value instanceof String str) {
      return truncate(str, MAX_STRING_LENGTH);
    }

    if (value instanceof byte[]) {
      return "[BINARY]";
    }

    return value;
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength) + "...(truncated)";
  }

  private ErrorResponse badRequestResponse() {
    return new ErrorResponse(
        java.time.Instant.now(),
        com.team3.monew.global.enums.ErrorCode.INVALID_INPUT_VALUE.name(),
        com.team3.monew.global.enums.ErrorCode.INVALID_INPUT_VALUE.getMessage(),
        Map.of(),
        400
    );
  }
}

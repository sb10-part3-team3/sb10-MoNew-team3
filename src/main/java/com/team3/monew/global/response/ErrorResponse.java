package com.team3.monew.global.response;

import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

public record ErrorResponse(
    Instant timestamp,
    String code,
    String message,
    Map<String, Object> details,
    int status
) {

  public static ErrorResponse of(BusinessException e) {
    return new ErrorResponse(
        Instant.now(),
        e.getErrorCode().name(),
        e.getMessage(),
        e.getDetails() != null ? e.getDetails() : Map.of(),
        e.getErrorCode().getStatus().value()
    );
  }

  public static ErrorResponse of(MethodArgumentNotValidException e) {
    Map<String, Object> details = new HashMap<>();

    e.getBindingResult().getFieldErrors().forEach(error -> {
      details.putIfAbsent(error.getField(), error.getDefaultMessage());
    });

    return new ErrorResponse(
        Instant.now(),
        ErrorCode.INVALID_INPUT_VALUE.name(),
        ErrorCode.INVALID_INPUT_VALUE.getMessage(),
        details,
        ErrorCode.INVALID_INPUT_VALUE.getStatus().value()
    );
  }

  public static ErrorResponse of(MethodArgumentTypeMismatchException e,
      Map<String, Object> details) {
    return new ErrorResponse(
        Instant.now(),
        ErrorCode.INVALID_PARAMETER_TYPE.name(),
        ErrorCode.INVALID_PARAMETER_TYPE.getMessage(),
        details != null ? details : Map.of(),
        ErrorCode.INVALID_PARAMETER_TYPE.getStatus().value()
    );
  }

  public static ErrorResponse of(Exception e) {
    return new ErrorResponse(
        Instant.now(),
        ErrorCode.INTERNAL_SERVER_ERROR.name(),
        ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
        Map.of(),
        HttpStatus.INTERNAL_SERVER_ERROR.value()
    );
  }
}

package com.team3.monew.global.exception;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  public BusinessException(ErrorCode errorCode) {
    this(errorCode, Map.of());
  }

  public BusinessException(ErrorCode errorCode, Map<String, Object> details) {
    super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage());
    this.errorCode = errorCode;
    this.details = details == null ? Map.of() : Map.copyOf(details);
  }
}

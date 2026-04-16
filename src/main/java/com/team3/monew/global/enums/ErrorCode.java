package com.team3.monew.global.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  EMAIL_DUPLICATION(HttpStatus.CONFLICT, "해당 이메일이 이미 존재합니다."),
  INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "해당 파라미터가 유효하지 않습니다."),
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 형식이 올바르지 않습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }
}

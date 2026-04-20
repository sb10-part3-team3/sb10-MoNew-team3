package com.team3.monew.exception.user;

import com.team3.monew.global.enums.ErrorCode;

public class AuthException extends UserException {

  public AuthException() {
    super(ErrorCode.INVALID_CREDENTIAL);
  }
}

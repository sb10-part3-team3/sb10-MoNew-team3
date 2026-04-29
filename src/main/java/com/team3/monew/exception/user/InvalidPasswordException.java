package com.team3.monew.exception.user;

import com.team3.monew.global.enums.ErrorCode;

public class InvalidPasswordException extends UserException{

  public InvalidPasswordException() {
    super(ErrorCode.INVALID_PASSWORD);
  }
}

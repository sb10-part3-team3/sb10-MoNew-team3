package com.team3.monew.exception.user;

import com.team3.monew.global.enums.ErrorCode;

public class InvalidNicknameException extends UserException{

  public InvalidNicknameException() {
    super(ErrorCode.INVALID_NICKNAME);
  }
}

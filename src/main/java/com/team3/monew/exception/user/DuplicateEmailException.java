package com.team3.monew.exception.user;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;

public class DuplicateEmailException extends UserException{

  public DuplicateEmailException() {
    super(ErrorCode.EMAIL_DUPLICATION);
  }
}

package com.team3.monew.exception;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;

public class DuplicateEmailException extends UserException{

  public DuplicateEmailException(String email) {
    super(ErrorCode.EMAIL_DUPLICATION, Map.of("email", email));
  }
}

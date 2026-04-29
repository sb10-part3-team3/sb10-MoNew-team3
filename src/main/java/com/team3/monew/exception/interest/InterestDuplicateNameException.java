package com.team3.monew.exception.interest;

import com.team3.monew.global.enums.ErrorCode;

import java.util.Map;

public class InterestDuplicateNameException extends InterestException {

  public InterestDuplicateNameException() {
    super(ErrorCode.INTEREST_NAME_DUPLICATE);
  }
}

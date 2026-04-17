package com.team3.monew.exception.interest;

import com.team3.monew.global.enums.ErrorCode;

import java.util.Map;

public class InterestNotFoundException extends InterestException {

  public InterestNotFoundException() {
    super(ErrorCode.INTEREST_NOT_FOUND);
  }
}

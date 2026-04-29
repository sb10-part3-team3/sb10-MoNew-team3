package com.team3.monew.exception.interest;

import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;

import java.util.Map;

public class InterestException extends BusinessException {

  public InterestException(ErrorCode errorCode) {
    super(errorCode);
  }

  public InterestException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}

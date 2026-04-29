package com.team3.monew.exception.useractivity;

import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import java.util.Map;

public class UserActivityException extends BusinessException {

  public UserActivityException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}

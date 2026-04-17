package com.team3.monew.exception.user;

import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import java.util.Map;

public class UserException extends BusinessException {
  public UserException(ErrorCode errorCode) {
    super(errorCode, Map.of());
  }

  public UserException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}

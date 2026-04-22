package com.team3.monew.exception.news;

import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import java.util.Map;

public class NewsException extends BusinessException {

  public NewsException(ErrorCode errorCode) {
    super(errorCode);
  }

  public NewsException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}

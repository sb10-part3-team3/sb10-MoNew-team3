package com.team3.monew.exception.news;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;

public class NewsIllegalBeanException extends NewsException {

  public NewsIllegalBeanException(Map<String, Object> details) {
    super(ErrorCode.INTERNAL_SERVER_ERROR, details);
  }
}

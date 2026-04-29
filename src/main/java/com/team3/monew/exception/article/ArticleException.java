package com.team3.monew.exception.article;

import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import java.util.Map;

public class ArticleException extends BusinessException {

  public ArticleException(ErrorCode errorCode) {
    super(errorCode, Map.of());
  }

  public ArticleException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}

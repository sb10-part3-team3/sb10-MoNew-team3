package com.team3.monew.exception.article;

import com.team3.monew.global.enums.ErrorCode;

public class ArticleInvalidPeriodException extends ArticleException {

  public ArticleInvalidPeriodException() {
    super(ErrorCode.ARTICLE_INVALID_PERIOD);
  }
}

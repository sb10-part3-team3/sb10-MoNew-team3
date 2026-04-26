package com.team3.monew.exception.article;

import com.team3.monew.global.enums.ErrorCode;

public class ArticleRequestInvalidException extends ArticleException {

  public ArticleRequestInvalidException() {
    super(ErrorCode.INVALID_INPUT_VALUE);
  }
}

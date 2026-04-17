package com.team3.monew.exception.article;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class ArticleNotFoundException extends ArticleException {

  public ArticleNotFoundException(UUID articleId) {
    super(ErrorCode.ARTICLE_NOT_FOUND, Map.of("articleId", articleId));
  }
}

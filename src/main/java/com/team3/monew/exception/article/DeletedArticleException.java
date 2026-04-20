package com.team3.monew.exception.article;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class DeletedArticleException extends ArticleException {

  public DeletedArticleException(UUID articleId) {
    super(ErrorCode.ARTICLE_DELETED, Map.of("articleId", articleId));
  }
}

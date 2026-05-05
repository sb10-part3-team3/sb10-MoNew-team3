package com.team3.monew.event;

import com.team3.monew.entity.ArticleView;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewEvent(
    UUID id,
    UUID userId,
    Instant createdAt,
    UUID articleId,
    String source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    Integer articleCommentCount,
    Integer articleViewCount,
    Boolean isFirstView
) {
  public static ArticleViewEvent from(ArticleView articleView, Boolean isFirstView) {
    return new ArticleViewEvent(
        articleView.getId(),
        articleView.getUser().getId(),
        articleView.getLastViewedAt(),
        articleView.getArticle().getId(),
        articleView.getArticle().getSource().getName(),
        articleView.getArticle().getOriginalLink(),
        articleView.getArticle().getTitle(),
        articleView.getArticle().getPublishedAt(),
        articleView.getArticle().getSummary(),
        articleView.getArticle().getCommentCount(),
        articleView.getArticle().getViewCount(),
        isFirstView
    );
  }
}

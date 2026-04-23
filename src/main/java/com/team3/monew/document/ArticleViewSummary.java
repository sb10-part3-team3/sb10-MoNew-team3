package com.team3.monew.document;

import com.team3.monew.entity.enums.NewsSourceType;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewSummary(
    UUID id,
    UUID viewedBy,
    Instant createdAt,
    UUID articleId,
    NewsSourceType source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    Long articleCommentCount,
    Long articleViewCount
) {

}

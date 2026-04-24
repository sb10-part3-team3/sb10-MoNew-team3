package com.team3.monew.document;

import java.time.Instant;
import java.util.UUID;

public record ArticleViewSummary(
    UUID id,
    UUID viewedBy,
    Instant createdAt,
    UUID articleId,
    String source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    Integer articleCommentCount,
    Integer articleViewCount
) {

}

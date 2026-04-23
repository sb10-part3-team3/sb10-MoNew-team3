package com.team3.monew.document;

import java.time.Instant;
import java.util.UUID;

public record CommentSummary(
    UUID id,
    UUID articleId,
    String articleTitle,
    UUID userId,
    String userNickname,
    String content,
    Long likeCount,
    Instant createdAt
) {

}

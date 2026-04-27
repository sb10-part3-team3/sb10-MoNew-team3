package com.team3.monew.document;

import java.time.Instant;
import java.util.UUID;

public record CommentLikeSummary(
    UUID id,
    Instant createdAt,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    Integer commentLikeCount,
    Instant commentCreatedAt
) {

}

package com.team3.monew.dto.comment;

import java.time.Instant;
import java.util.UUID;

public record CommentLikeDto(
        UUID id,
        UUID likedBy,
        Instant createdAt,
        UUID commentId,
        UUID articleId,
        UUID commentUserId,
        String commentUserNickname,
        String commentContent,
        Long commentLikeCount,
        Instant commentCreatedAt
) {
}

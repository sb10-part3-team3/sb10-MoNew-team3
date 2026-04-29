package com.team3.monew.event;

import com.team3.monew.entity.Comment;
import java.time.Instant;
import java.util.UUID;

public record CommentRegisteredEvent(
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID userId,
    String userNickname,
    String content,
    Integer likeCount,
    Instant createdAt
) {
  public static CommentRegisteredEvent from(Comment comment) {
    return new CommentRegisteredEvent(
        comment.getId(),
        comment.getArticle().getId(),
        comment.getArticle().getTitle(),
        comment.getUser().getId(),
        comment.getUser().getNickname(),
        comment.getContent(),
        comment.getLikeCount(),
        comment.getCreatedAt()
    );
  }
}

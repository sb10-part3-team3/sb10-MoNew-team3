package com.team3.monew.event;

import com.team3.monew.entity.Comment;
import com.team3.monew.entity.CommentLike;
import java.time.Instant;
import java.util.UUID;

public record CommentLikedActivityEvent(
    UUID actorUserId, //좋아요를 누른 사용자
    UUID commentId,
    UUID id,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    Integer commentLikeCount,
    Instant commentCreatedAt
) {

  public static CommentLikedActivityEvent from(CommentLike commentLike) {
    Comment comment = commentLike.getComment();

    return new CommentLikedActivityEvent(
        commentLike.getUser().getId(),
        comment.getId(),
        commentLike.getId(),
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
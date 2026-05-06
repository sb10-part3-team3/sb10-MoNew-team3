package com.team3.monew.event;

import java.util.UUID;

public record CommentLikedEvent(
    UUID actorUserId, //좋아요를 누른 사용자
    UUID commentId
) {
  public static CommentLikedEvent of(UUID actorUserId, UUID commentId) {
    return new CommentLikedEvent(
        actorUserId,
        commentId
    );
  }
}
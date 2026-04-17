package com.team3.monew.event;

import java.util.UUID;

public record CommentLikedEvent(
    UUID actorUserId, //좋아요를 누른 사용자
    UUID commentId,
    UUID writerId // 댓글 작성자
) {

}
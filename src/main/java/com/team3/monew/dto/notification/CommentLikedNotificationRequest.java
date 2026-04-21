package com.team3.monew.dto.notification;

import java.util.UUID;

public record CommentLikedNotificationRequest(
    UUID actorUserId,
    UUID commentId
) {

}

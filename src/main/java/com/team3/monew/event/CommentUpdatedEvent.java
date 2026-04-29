package com.team3.monew.event;

import java.util.UUID;

public record CommentUpdatedEvent(
    UUID commentId,
    UUID userId,
    String content
) {

}

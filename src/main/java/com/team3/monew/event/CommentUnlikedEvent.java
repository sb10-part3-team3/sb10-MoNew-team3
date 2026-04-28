package com.team3.monew.event;

import java.util.UUID;

public record CommentUnlikedEvent(
    UUID userId,
    UUID commentLikeId
) {

}

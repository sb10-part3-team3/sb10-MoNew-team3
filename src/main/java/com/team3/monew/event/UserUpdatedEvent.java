package com.team3.monew.event;

import java.util.UUID;

public record UserUpdatedEvent(
    UUID userId,
    String nickname
) {

}

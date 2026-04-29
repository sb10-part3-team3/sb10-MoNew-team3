package com.team3.monew.document;

import java.time.Instant;
import java.util.UUID;

public record UserActivityRequest(
    UUID id,
    String email,
    String nickname,
    Instant createdAt
) {

}

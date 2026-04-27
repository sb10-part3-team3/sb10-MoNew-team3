package com.team3.monew.event;

import com.team3.monew.entity.User;
import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
    UUID userId,
    String email,
    String nickname,
    Instant createdAt
) {
  public static UserRegisteredEvent from(User user) {
    return new UserRegisteredEvent(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getCreatedAt()
    );
  }
}

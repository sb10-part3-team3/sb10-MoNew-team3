package com.team3.monew.event;

import com.team3.monew.entity.Subscription;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionEvent(
    UUID subscriptionId,
    UUID interestId,
    UUID userId,
    String interestName,
    List<String> interestKeywords,
    Integer interestSubscriberCount,
    Instant createdAt
) {
  public static SubscriptionEvent from(Subscription subscription) {
    return new SubscriptionEvent(
      subscription.getId(),
      subscription.getInterest().getId(),
      subscription.getUser().getId(),
      subscription.getInterest().getName(),
      subscription.getInterest().getStringKeywords(),
      subscription.getInterest().getSubscriberCount(),
      subscription.getCreatedAt()
    );
  }
}

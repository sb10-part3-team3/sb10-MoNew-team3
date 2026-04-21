package com.team3.monew.dto.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record InterestNotificationRequest(
    UUID interestId,
    String interestName,
    Integer articleCount,
    List<UUID> subscriberIds
) {

  public InterestNotificationRequest {
    subscriberIds = subscriberIds == null ? new ArrayList<UUID>() : List.copyOf(subscriberIds);
  }
}

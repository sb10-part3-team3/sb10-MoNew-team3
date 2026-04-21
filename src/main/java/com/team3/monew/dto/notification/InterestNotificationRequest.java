package com.team3.monew.dto.notification;

import java.util.List;
import java.util.UUID;

public record InterestNotificationRequest(
    UUID interestId,
    String interestName,
    Integer articleCount,
    List<UUID> subscriberIds
) {

}

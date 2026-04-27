package com.team3.monew.document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionSummary(
  UUID id,
  UUID interestId,
  String interestName,
  List<String> interestKeywords,
  Integer interestSubscriberCount,
  Instant createdAt
) {

}

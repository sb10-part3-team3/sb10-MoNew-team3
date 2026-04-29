package com.team3.monew.event;

import java.util.Map;
import java.util.UUID;

public record InterestNotificationEvent(
    Map<UUID, InterestArticleSummary> interestArticleSummaryMap
) {

  public record InterestArticleSummary(
      String interestName,
      Integer articleCount
  ) {

  }

}
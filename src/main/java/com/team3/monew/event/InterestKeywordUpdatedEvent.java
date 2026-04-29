package com.team3.monew.event;

import java.util.List;
import java.util.UUID;

public record InterestKeywordUpdatedEvent(
    UUID interestId,
    List<String> keywords
) {

}

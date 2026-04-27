package com.team3.monew.dto.interest;

import java.util.List;
import java.util.UUID;

public record InterestDto(
    UUID id,
    String name,
    List<String> keywords,
    int subscriberCount,
    Boolean subscribedByMe
) {

}

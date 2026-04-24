package com.team3.monew.dto.interest.internal;

public record InterestSearchCondition(
    String keyword,
    String orderBy,
    String direction,
    InterestCursor cursor,
    int limit
) {

}

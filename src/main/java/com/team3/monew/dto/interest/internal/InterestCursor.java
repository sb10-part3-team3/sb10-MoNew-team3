package com.team3.monew.dto.interest.internal;

import java.time.Instant;

public record InterestCursor(
    String cursor,
    Instant after
) {

}

package com.team3.monew.dto.interest;

import java.util.List;

public record CursorPageResponseInterestDto(
    List<InterestDto> content,
    String nextCursor,
    String nextAfter,
    int size,
    int totalElements,
    boolean hasNext
) {

}

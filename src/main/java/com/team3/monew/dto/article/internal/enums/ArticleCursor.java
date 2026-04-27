package com.team3.monew.dto.article.internal.enums;

import java.time.Instant;

public record ArticleCursor(
    Object cursor,
    Instant after
) {

}

package com.team3.monew.dto.article.internal;

import java.time.Instant;

public record ArticleCursor(
    Object cursor,
    Instant after
) {

}

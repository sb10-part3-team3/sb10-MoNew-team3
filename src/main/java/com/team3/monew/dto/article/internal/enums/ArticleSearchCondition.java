package com.team3.monew.dto.article.internal.enums;

import com.team3.monew.entity.enums.NewsSourceType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleSearchCondition(
    String keyword,
    UUID interestId,
    List<NewsSourceType> sourceIn,
    Instant publishDateFrom,
    Instant publishDateTo,
    ArticleOrderBy articleOrderBy,
    ArticleDirection direction,
    ArticleCursor cursor,
    Integer limit
) {

}

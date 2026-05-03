package com.team3.monew.dto.article;

import com.team3.monew.entity.enums.NewsSourceType;
import java.time.Instant;

public record ArticleBackup(
    NewsSourceType sourceType,
    String originalLink,
    String title,
    Instant publishedAt,
    String summary
) {

}

package com.team3.monew.component.news.record;

import com.team3.monew.entity.enums.NewsSourceType;
import java.time.Instant;
import java.util.List;

public record ParsedNewsArticle(NewsSourceType sourceType,
                                String link,
                                String title,
                                Instant publishedAt,
                                String summary,
                                List<String> keywords) {

}

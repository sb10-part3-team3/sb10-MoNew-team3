package com.team3.monew.component.news.record;

import com.team3.monew.entity.enums.NewsSourceType;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public record ParsedData(NewsSourceType sourceType,
                         Instant lastBuildDate,
                         int page,
                         List<ParsedNewsArticle> articles) {

  public static ParsedData createEmpty() {
    return new ParsedData(null, null, 0, Collections.emptyList());
  }

  public boolean isEmpty() {
    return this.articles == null || this.articles.isEmpty();
  }
}

package com.team3.monew.component.news.client;

import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.enums.NewsSourceType;
import java.util.List;
import java.util.Set;
import reactor.core.publisher.Mono;

public interface NewsClient {

  NewsSourceType getSourceType();

  Mono<List<ParsedNewsArticle>> fetchAndProcess(Set<String> keywords);
}

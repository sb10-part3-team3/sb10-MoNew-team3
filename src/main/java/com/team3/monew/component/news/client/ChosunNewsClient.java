package com.team3.monew.component.news.client;

import com.team3.monew.component.news.collect.NewsCollector;
import com.team3.monew.component.news.filter.NewsFilter;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.enums.NewsSourceType;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChosunNewsClient implements NewsClient {

  private final NewsCollector newsCollector;
  private final NewsFilter newsFilter;

  @Override
  public NewsSourceType getSourceType() {
    return NewsSourceType.CHOSUN;
  }

  @Override
  public Mono<List<ParsedNewsArticle>> fetchAndProcess(Set<String> keywords) {
    // CHOSUN은 단일 쿼리만 있음
    return newsCollector.collectRawNews(getSourceType(), keywords)
        .map(newsFilter::filterKeyword)
        .flatMapIterable(list -> list)       // 단일 쿼리지만 구조를 맞추기 위해 사용
        .collectList();
  }
}

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
public class NaverNewsClient implements NewsClient {

  private final NewsCollector newsCollector;
  private final NewsFilter newsFilter;

  @Override
  public NewsSourceType getSourceType() {
    return NewsSourceType.NAVER;
  }

  @Override
  public Mono<List<ParsedNewsArticle>> fetchAndProcess(Set<String> keywords) {
    // Naver는 키워드별로 쿼리를 여러개 보냄
    return newsCollector.collectRawNews(getSourceType(), keywords)
        .map(newsFilter::filterKeyword)
        .flatMapIterable(list -> list)       // 여러개의 쿼리 결과값을 하나로 합침
        .distinct(ParsedNewsArticle::link)                       // 키워드별 쿼리에 중복 가능성이 있어 제거
        .collectList();
  }
}

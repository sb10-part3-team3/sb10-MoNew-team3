package com.team3.monew.component.news.client;

import com.team3.monew.component.news.collect.NewsCollector;
import com.team3.monew.component.news.filter.NewsFilter;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.enums.NewsSourceType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  public Mono<List<ParsedNewsArticle>> fetchAndProcess(
      Collection<InterestKeyword> interestKeywords) {
    // Naver는 키워드별로 쿼리를 여러개 보냄
    return newsCollector.collectRawNews(getSourceType(), interestKeywords)
        .map(parsedData -> newsFilter.filterKeyword(parsedData, interestKeywords))
        .flatMapIterable(list -> list)       // 여러개의 쿼리 결과값을 하나로 합침
        .collect(Collectors.toMap(
            ParsedNewsArticle::link,
            article -> article,
            (existing, replacement) -> {
              // 키워드별 쿼리에 중복 가능성이 있는데 더 많은 관심사 있는 쪽을 살림

              List<InterestKeyword> merged = Stream.concat(
                      existing.interestKeywords().stream(),
                      replacement.interestKeywords().stream())
                  .collect(Collectors.toMap(
                      ik -> ik.getInterest().getId() + ":" + ik.getKeyword(), // 중복기준 Key
                      ik -> ik,
                      (oldValue, newValue) -> oldValue
                  ))
                  .values()
                  .stream()
                  .toList();

              return new ParsedNewsArticle(
                  existing.sourceType(),
                  existing.link(),
                  existing.title(),
                  existing.publishedAt(),
                  existing.summary(),
                  merged
              );
            }
        ))
        .map(value -> new ArrayList<>(value.values()));
  }
}

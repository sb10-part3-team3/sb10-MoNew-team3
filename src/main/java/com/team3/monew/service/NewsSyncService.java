package com.team3.monew.service;

import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.InterestKeywordRepository;
import com.team3.monew.repository.NewsSourceRepository;
import com.team3.monew.service.NewsParseService.ParsedData;
import com.team3.monew.service.NewsParseService.ParsedNewsArticle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSyncService {

  private final InterestKeywordRepository interestKeywordRepository;
  private final KeywordMatchService keywordMatchService;
  private final NewsSourceRepository newsSourceRepository;
  private final NewsCollectService newsCollectService;
  private final NewsFilterService newsFilterService;
  private final NewsSaveService newsSaveService;

  public void executeNewsCollectionWorkFlow() {
    // 키워드를 기준으로 관심사 묶기
    Map<String, List<InterestKeyword>> keywordInterests = interestKeywordRepository.findAll()
        .stream()
        .collect(Collectors.groupingBy(
            InterestKeyword::getKeyword,    // keyword가 key
            Collectors.toList()             // keyword를 갖는 관심사 리스트
        ));

    // 키워드 세팅
    Set<String> keywords = keywordInterests.keySet();
    keywordMatchService.refreshKeywords(keywords);

    // 뉴스 수집하기위한 NewsSource
    List<NewsSource> sources = newsSourceRepository.findAll();

    Flux.fromIterable(sources)
        .flatMap(source ->
                newsCollectService.collectRawNews(source, keywords),
            sources.size())                       // NewsSources 개수만큼 동시성 수행
        .groupBy(ParsedData::sourceType)          // Source별로 그룹화
        .flatMap(groupedSource -> groupedSource
                .collectList()      // 하나의 그룹의 여러 쿼리 합치기
                .map(groupedData ->
                    newsFilterService.filter(groupedSource.key(), groupedData, keywords)),
            NewsSourceType.values().length)       // NewsSource 개수만큼 동시성 수행
        .collectList()              // List.of(List('A소스 기사'), List('B소스 기사'))
        .map(listInList -> {
          // 중첩 리스트 하나로 합치기
          List<ParsedNewsArticle> allArticles = listInList.stream()
              .flatMap(List::stream)
              .toList();

          // Link가 같으면 Naver 우선
          Collection<ParsedNewsArticle> distinctArticles = allArticles.stream()
              .collect(Collectors.toMap(
                  ParsedNewsArticle::link,            //   key: link
                  article -> article,  // value: ParsedNewsArticle
                  (existing, replacement) ->
                      // merge function. 중복일 시 Naver우선으로 충돌 해결
                      existing.sourceType().equals(NewsSourceType.NAVER)
                          ? existing
                          : replacement
              )).values();

          return new ArrayList<>(distinctArticles);
        })
        .publishOn(Schedulers.boundedElastic())   // 동기저장을 위한 전용 스레드 플로 넘기기
        .map(newsSaveService::save)
        .publishOn(Schedulers.parallel())         // 다시 원래 스레드로 복구
//        .map(k -> 저장된 데이터 알람처리)
        .subscribe();
  }
}

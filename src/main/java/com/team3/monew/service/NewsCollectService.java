package com.team3.monew.service;

import com.team3.monew.component.news.client.NewsClient;
import com.team3.monew.component.news.filter.KeywordMatch;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.monitoring.BatchMetrics;
import com.team3.monew.repository.InterestKeywordRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCollectService {

  private final InterestKeywordRepository interestKeywordRepository;
  private final KeywordMatch keywordMatch;
  private final Map<String, NewsClient> newsClients;
  private final NewsSaveService newsSaveService;
  private final BatchMetrics batchMetrics;

  @Scheduled(cron = "${app.cron.news-collections}")
  public void scheduleNewsJob() {
    executeNewsCollection()
        .doOnError(e -> log.error("뉴스 수집 스케줄 작업 실패", e))
        .subscribe();
  }

  public Mono<Void> executeNewsCollection() {
    long startTime = System.currentTimeMillis();
    log.debug("뉴스 기사 수집 시작");

    return Mono.fromCallable(() -> {
          // join쓰는 고비용 작업은 boundedElastic 스레드
          List<InterestKeyword> interestKeywordList = interestKeywordRepository.findAllWithInterest();

          // 관심사와, 키워드를 하나로 묶기
          Set<String> keywords = interestKeywordList.stream()
              .flatMap(ik -> Stream.of(ik.getInterest().getName(), ik.getKeyword()))
              .collect(Collectors.toSet());
          keywordMatch.refreshKeywords(keywords);

          return interestKeywordList;
        })
        .subscribeOn(
            Schedulers.boundedElastic())   // 위쪽 fromCallable 함수가 boundedElastic 스레드에서 돌아가게 진행
        // Mono(단일 데이터) -> Flux(n개 데이터)
        .flatMapMany(interestKeywords ->
            Flux.fromIterable(newsClients.values())
                // 수집, 파싱, 필터까지 수행, NewsSources 개수만큼 동시성 수행
                .flatMap(client -> client.fetchAndProcess(interestKeywords), newsClients.size())
                .flatMapIterable(list -> list)
                .collectList()                            // 여러개의 NewsSource 결과값을 하나로 합침
                .map(this::deduplicateByLinkWithNaverPriorityAndOrderByPublishAtDesc)
                .publishOn(Schedulers.boundedElastic())   // 동기저장을 위한 전용 스레드 플로 넘기기
                .map(newsSaveService::saveAndNotify)
                .doOnSuccess(savedArticles -> batchMetrics.recordNewsCollectSuccess(
                    System.currentTimeMillis() - startTime,
                    savedArticles.size()
                ))
                .doOnError(error -> batchMetrics.recordNewsCollectFailure(
                    System.currentTimeMillis() - startTime
                ))
                .publishOn(Schedulers.parallel())         // 다시 원래 스레드로 복구
                .doOnSuccess(unused ->
                    log.info("뉴스 기사 수집 종료: {}ms", System.currentTimeMillis() - startTime))
        ).then();
  }

  private List<ParsedNewsArticle> deduplicateByLinkWithNaverPriorityAndOrderByPublishAtDesc(
      List<ParsedNewsArticle> list) {
    return list.stream()
        .collect(Collectors.toMap(
            ParsedNewsArticle::link,               //   key: link
            article -> article,     // value: ParsedNewsArticle
            (existing, replacement) ->
                // merge function. 중복일 시 Naver우선으로 충돌 해결
                NewsSourceType.NAVER.equals(existing.sourceType())
                    ? existing
                    : replacement
        ))
        .values()
        .stream()
        .sorted(Comparator.comparing(ParsedNewsArticle::publishedAt).reversed())
        .toList();
  }
}

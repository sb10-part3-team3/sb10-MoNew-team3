package com.team3.monew.service;

import com.team3.monew.component.news.client.NewsClient;
import com.team3.monew.component.news.filter.KeywordMatch;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.InterestKeywordRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
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

  @Scheduled(cron = "${app.cron.news-collections}")
  public void scheduleNewsJob() {
    executeNewsCollection()
        .doOnError(e -> log.error("뉴스 수집 스케줄 작업 실패", e))
        .subscribe();
  }

  public Mono<Void> executeNewsCollection() {
    AtomicLong startTime = new AtomicLong();
    startTime.set(System.currentTimeMillis());
    log.debug("뉴스 기사 수집 시작");

    // 키워드를 기준으로 관심사 묶기
    Map<String, List<InterestKeyword>> keywordInterests = interestKeywordRepository.findAll()
        .stream()
        .collect(Collectors.groupingBy(
            InterestKeyword::getKeyword,    // keyword가 key
            Collectors.toList()             // keyword를 갖는 관심사 리스트
        ));

    // 키워드 세팅
    Set<String> keywords = keywordInterests.keySet();
    keywordMatch.refreshKeywords(keywords);

    return Flux.fromIterable(newsClients.values())
        // 수집, 파싱, 필터까지 수행, NewsSources 개수만큼 동시성 수행
        .flatMap(client -> client.fetchAndProcess(keywords), newsClients.size())
        .flatMapIterable(list -> list)
        .collectList()                            // 여러개의 NewsSource 결과값을 하나로 합침
        .map(this::deduplicateByLinkWithNaverPriority)
        .publishOn(Schedulers.boundedElastic())   // 동기저장을 위한 전용 스레드 플로 넘기기
        .map(newsSaveService::save)
        .publishOn(Schedulers.parallel())         // 다시 원래 스레드로 복구
        .doOnSuccess(result ->
            log.info("뉴스 기사 수집 종료: {}ms", System.currentTimeMillis() - startTime.get()))
//        .map(k -> 저장된 데이터 알람처리)
        .then();
  }

  private List<ParsedNewsArticle> deduplicateByLinkWithNaverPriority(List<ParsedNewsArticle> list) {
    return list.stream()
        .collect(Collectors.toMap(
            ParsedNewsArticle::link,               //   key: link
            article -> article,     // value: ParsedNewsArticle
            (existing, replacement) ->
                // merge function. 중복일 시 Naver우선으로 충돌 해결
                existing.sourceType().equals(NewsSourceType.NAVER)
                    ? existing
                    : replacement
        ))
        .values()
        .stream()
        .toList();
  }
}

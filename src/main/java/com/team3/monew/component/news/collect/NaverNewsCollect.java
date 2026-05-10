package com.team3.monew.component.news.collect;

import com.team3.monew.component.news.parse.NewsParser;
import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.RawArticleResult;
import com.team3.monew.config.NaverProperties;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.exception.news.NewsClientException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Slf4j
@Component
@EnableConfigurationProperties(NaverProperties.class)
@RequiredArgsConstructor
public class NaverNewsCollect implements NewsCollect {

  private final NaverProperties naverProperties;
  private final NewsParser newsParser;

  // Test를 위한 비 상수화
  @Value("${news.naver.base-url:https://openapi.naver.com}")
  private String naverBaseUrl;
  private static final String NAVER_QUERY_PATH = "/v1/search/news.json";
  private static final int NAVER_QUERY_DISPLAY = 100;     // 한번에 표시할 갯수(10~100) default: 10
  private static final int NAVER_QUERY_START = 1;         // 검색 시작 위치(1~1000)    default: 1
  private static final String NAVER_QUERY_SORT = "date";  // 검색결과 정렬 내림차순(sim:정확도, date:날짜) default: sim

  private static final int NAVER_CONCURRENCY_SIZE = 2;
  private final RequestGate requestGate = new RequestGate(new RateLimitState());

  // keyword별 검색시간(Key형태: '관심사__키워드')
  private final Map<String, Instant> lastCollectedAt = new ConcurrentHashMap<>();

  @Override
  public Flux<ParsedData> collect(WebClient webClient, NewsSourceType sourceType,
      Collection<InterestKeyword> interestKeywords) {

    return Flux.fromIterable(interestKeywords)
        .flatMap(interestKeyword -> {

              return fetchNewsData(webClient, interestKeyword, 1)
                  .flatMap(raw -> {
                    ParsedData parsedData = newsParser.parse(sourceType, raw);
                    return parsedData.isEmpty() ? Mono.empty() : Mono.just(parsedData);
                  })
                  // 재귀 확장용
                  .expand(data -> {
                    // 파싱 오류로 나온 빈값 검사
                    if (data.articles().isEmpty()) {
                      return Mono.empty();
                    }

                    // 키워드의 첫 다운로드면 통과
                    String keyword = resolveKeyword(interestKeyword);
                    if (!lastCollectedAt.containsKey(keyword)) {
                      lastCollectedAt.put(keyword, data.lastBuildDate());
                      log.info("Naver 뉴스기사 collect 성공 - interest={}, keyword={}",
                          interestKeyword.getInterest().getName(), interestKeyword.getKeyword());
                      return Mono.empty();
                    }

                    Instant lastPublishedAt = data.articles().get(data.articles().size() - 1)
                        .publishedAt();
                    // API 호출시간이 마지막 기사 발행시간보다 과거거나
                    // 같다면 재귀호출(네이버는 기사발행시간이 분단위로 절삭되어 비교해야 함)
                    if (lastCollectedAt.get(keyword).isBefore(lastPublishedAt) ||
                        lastCollectedAt.get(keyword).equals(lastPublishedAt)) {
                      log.debug("{}번째 페이지 요청", data.page() + 1);
                      return fetchNewsData(webClient, interestKeyword, data.page() + 1)
                          .map(raw -> newsParser.parse(sourceType, raw))
                          .filter(nextData -> !nextData.articles().isEmpty());
                    }

                    // API 호출시간이 기사 리스트 안쪽에 포함되어있다면, 즉 이미 봤다고 가정하면
                    lastCollectedAt.put(keyword, data.lastBuildDate());
                    log.info("Naver 뉴스기사 collect 성공 - interest={}, keyword={}",
                        interestKeyword.getInterest().getName(), interestKeyword.getKeyword());
                    return Mono.empty();
                  });
            }
            , NAVER_CONCURRENCY_SIZE); // 동시성 제한

  }

  private Mono<RawArticleResult> fetchNewsData(
      WebClient webClient, InterestKeyword interestKeyword, int page) {
    String fullBaseUrl = naverBaseUrl.contains("://") ? naverBaseUrl : "http://" + naverBaseUrl;
    String queryKeyword =
        interestKeyword.getInterest().getName() + " " + interestKeyword.getKeyword();

    return Mono.usingWhen(
        requestGate.acquirePermit(),
        permit ->
            // burst 방지
            Mono.delay(Duration.ofMillis(100))
                .then(
                    webClient.get().uri(builder ->
                            URI.create(fullBaseUrl + builder
                                .path(NAVER_QUERY_PATH)
                                .queryParam("query", queryKeyword)
                                .queryParam("display", NAVER_QUERY_DISPLAY)
                                .queryParam("start", NAVER_QUERY_DISPLAY * (page - 1) + 1)
                                .queryParam("sort", NAVER_QUERY_SORT)
                                .build())
                        )
                        .header("X-Naver-Client-Id", naverProperties.getId())
                        .header("X-Naver-Client-Secret", naverProperties.getSecret())
                        .exchangeToMono(response -> response.toEntity(String.class))
                )
                .map(entity -> {

                  HttpHeaders headers = entity.getHeaders();

                  int remaining = Optional.ofNullable(headers.getFirst("x-rate-limit-remaining"))
                      .map(Integer::parseInt)
                      .orElse(1);
                  long reset = Optional.ofNullable(headers.getFirst("x-rate-limit-reset"))
                      .map(Long::parseLong)
                      .orElse(System.currentTimeMillis());
                  log.debug("remaining={}, reset={}", remaining, reset);
                  requestGate.getRateLimitState().update(remaining, reset);

                  HttpStatusCode status = entity.getStatusCode();
                  String body = Optional.ofNullable(entity.getBody()).orElse("");

                  if (status.is4xxClientError()) {
                    boolean retryable = status.value() == 429;
                    throw new NewsClientException("Naver 요청 실패(4xx): " + body, retryable);
                  }

                  if (status.is5xxServerError()) {
                    throw new NewsClientException("Naver 서버 일시적 장애(5xx): " + body, true);
                  }

                  return body;
                })
                .timeout(Duration.ofSeconds(3)) // 전체 응답 대기 시간
                // 재시도 전략(최대 2번, 0.5초 간격)
                .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(500))
                    // 400번대 에러는 재시도 전략에서 제거 || 타임아웃 재시도 부여
                    .filter(ex ->
                        (ex instanceof NewsClientException ncs && ncs.isRetryable()) ||
                            ex instanceof TimeoutException
                    )
                )
                .onErrorResume(e -> {
                  log.error("Naver 기사 수집 실패: interest={}, keyword={}, error={}",
                      interestKeyword.getInterest().getName(),
                      interestKeyword.getKeyword(),
                      e.getMessage());

                  return Mono.empty();
                })
                .map(rawData -> {
                  log.info("Naver 뉴스기사 rawData 받기 성공 - interest={}, keyword={}, page={}",
                      interestKeyword.getInterest().getName(),
                      interestKeyword.getKeyword(),
                      page);

                  return new RawArticleResult(rawData, queryKeyword, page);
                }),
        requestGate::release
    );
  }


  private String resolveKeyword(InterestKeyword interestKeyword) {
    return String.format("%s__%s",
        interestKeyword.getInterest().getName(), interestKeyword.getKeyword());
  }

  static class RequestGate {

    private final RateLimitState state;
    private final Semaphore semaphore = new Semaphore(2);

    public RequestGate(RateLimitState state) {
      this.state = state;
    }

    public Mono<Permit> acquirePermit() {
      return Mono.fromCallable(() -> {
        semaphore.acquire();

        // rate limit 체크
        if (state.shouldWait()) {
          long wait = state.resetAt() - System.currentTimeMillis();

          if (wait > 0) {
            Thread.sleep(Math.min(wait, 1000)); // 상한선 추가
          }
        }

        return new Permit();
      }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> release(Permit permit) {
      semaphore.release();
      return Mono.empty();
    }

    public RateLimitState getRateLimitState() {
      return state;
    }
  }

  static final class Permit {

  }

  static class RateLimitState {

    private final AtomicInteger remaining = new AtomicInteger();
    private final AtomicLong resetAt = new AtomicLong();

    public void update(int remaining, long resetAt) {
      this.remaining.set(remaining);
      this.resetAt.set(resetAt);
    }

    public long resetAt() {
      return resetAt.get();
    }

    public boolean shouldWait() {
      return remaining.get() <= 6;    // 사후대처라 미리 널널하게 막음
    }
  }
}

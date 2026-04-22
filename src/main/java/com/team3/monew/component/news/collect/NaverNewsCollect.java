package com.team3.monew.component.news.collect;

import com.team3.monew.component.news.parse.NewsParser;
import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.RawArticleResult;
import com.team3.monew.config.NaverProperties;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.exception.news.NewsClientException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
@EnableConfigurationProperties(NaverProperties.class)
@RequiredArgsConstructor
public class NaverNewsCollect implements NewsCollect {

  private final NaverProperties naverProperties;
  private final NewsParser newsParser;

  // Test를 위한 비 상수화
  @Value("${news.chosun.base-url:https://openapi.naver.com}")
  private String naverBaseUrl;
  private static final String NAVER_QUERY_PATH = "/v1/search/news.json";
  private static final int NAVER_QUERY_DISPLAY = 100;     // 한번에 표시할 갯수(10~100) default: 10
  private static final int NAVER_QUERY_START = 1;         // 검색 시작 위치(1~1000)    default: 1
  private static final String NAVER_QUERY_SORT = "date";  // 검색결과 정렬 내림차순(sim:정확도, date:날짜) default: sim

  private static final int NAVER_CONCURRENCY_SIZE = 5;

  // keyword별 검색시간
  private final Map<String, Instant> lastCollectedAt = new ConcurrentHashMap<>();

  @Override
  public Flux<ParsedData> collect(WebClient webClient, NewsSourceType sourceType,
      Set<String> keywords) {

    return Flux.fromIterable(keywords)
        .flatMap(keyword ->
                fetchNewsData(webClient, keyword, 1)
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
                      if (!lastCollectedAt.containsKey(keyword)) {
                        lastCollectedAt.put(keyword, data.lastBuildDate());
                        log.info("Naver 뉴스기사 collect 성공 - keyword={}", keyword);
                        return Mono.empty();
                      }

                      Instant lastPublishedAt = data.articles().get(data.articles().size() - 1)
                          .publishedAt();
                      // API 호출시간이 마지막 기사 발행시간보다 과거거나
                      // 같다면 재귀호출(네이버는 기사발행시간이 분단위로 절삭되어 비교해야 함)
                      if (lastCollectedAt.get(keyword).isBefore(lastPublishedAt)
                          || lastCollectedAt.get(keyword).equals(lastPublishedAt)) {
                        log.debug("{}번째 페이지 요청", data.page() + 1);
                        return fetchNewsData(webClient, keyword, data.page() + 1)
                            .map(raw -> newsParser.parse(sourceType, raw))
                            .filter(nextData -> !nextData.articles().isEmpty());
                      }

                      // API 호출시간이 기사 리스트 안쪽에 포함되어있다면, 즉 이미 봤다고 가정하면
                      lastCollectedAt.put(keyword, data.lastBuildDate());
                      log.info("Naver 뉴스기사 collect 성공 - keyword={}", keyword);
                      return Mono.empty();
                    })
            , NAVER_CONCURRENCY_SIZE); // 동시성 제한

  }


  private Mono<RawArticleResult> fetchNewsData(
      WebClient webClient, String keyword, int page) {
    String fullBaseUrl = naverBaseUrl.contains("://") ? naverBaseUrl : "http://" + naverBaseUrl;

    return webClient.get().uri(builder ->
            URI.create(fullBaseUrl + builder
                .path(NAVER_QUERY_PATH)
                .queryParam("query", keyword)
                .queryParam("display", NAVER_QUERY_DISPLAY)
                .queryParam("start", NAVER_QUERY_DISPLAY * (page - 1) + 1)
                .queryParam("sort", NAVER_QUERY_SORT)
                .build())
        )
        .header("X-Naver-Client-Id", naverProperties.getId())
        .header("X-Naver-Client-Secret", naverProperties.getSecret())
        .retrieve()
        // 400번대 에러
        .onStatus(HttpStatusCode::is4xxClientError, response ->
            // 바디를 읽지 않고 비움처리 후에 에러 전파
            response.releaseBody()
                .then(Mono.error(new NewsClientException("Chosun 요청 실패(4xx)", false)))
        )
        // 500번대 에러
        .onStatus(HttpStatusCode::is5xxServerError, response ->
            response.releaseBody()
                .then(Mono.error(new NewsClientException("Chosun 서버 일시적 장애(5xx)", true)))
        )
        .bodyToMono(String.class)
        // 재시도 전략(최대 2번, 0.5초 간격)
        .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(500))
            // 400번대 에러는 재시도 전략에서 제거
            .filter(throwable -> throwable instanceof NewsClientException ncs && ncs.isRetryable())
        )
        .onErrorResume(e -> {
          log.error("Naver 기사 수집 실패: keyword={}, error={}", keyword, e.getMessage());
          return Mono.empty();
        })
        .map(rawData -> {
          log.info("Naver 뉴스기사 rawData 받기 성공 - keyword={}, page={}", keyword, page);
          return new RawArticleResult(rawData, keyword, page);
        });
  }
}

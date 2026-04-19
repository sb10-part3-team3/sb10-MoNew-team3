package com.team3.monew.service.news.collect;

import com.team3.monew.config.NaverProperties;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.service.NewsParseService;
import com.team3.monew.service.NewsParseService.ParsedData;
import com.team3.monew.service.news.record.RawArticleResult;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@EnableConfigurationProperties(NaverProperties.class)
@RequiredArgsConstructor
public class NaverNewsCollect implements NewsCollect {

  private final NaverProperties naverProperties;
  private final NewsParseService newsParseService;

  private static final String NAVER_QUERY_PATH = "/v1/search/news.json";
  private static final int NAVER_QUERY_DISPLAY = 100;     // 한번에 표시할 갯수(10~100) default: 10
  private static final int NAVER_QUERY_START = 1;         // 검색 시작 위치(1~1000)    default: 1
  private static final String NAVER_QUERY_SORT = "date";  // 검색결과 정렬 내림차순(sim:정확도, date:날짜) default: sim

  // keyword별 검색시간
  private final Map<String, Instant> lastCollectedAt = new HashMap<>();

  @Override
  public Flux<ParsedData> collect(WebClient webClient, NewsSource source, Set<String> keywords) {

    return Flux.fromIterable(keywords)
        .flatMap(keyword ->
                fetchNewsData(webClient, source.getBaseUrl(), keyword, 1)
                    .map(raw -> newsParseService.parse(NewsSourceType.NAVER, raw))
                    .expand(data -> {
                      // 키워드의 첫번째 기사 다운로드면 통과
                      if (!lastCollectedAt.containsKey(keyword)) {
                        lastCollectedAt.put(keyword, data.lastBuildDate());
                        return Mono.empty();
                      }

                      Instant lastPublishedAt = data.articles().get(data.articles().size() - 1)
                          .publishedAt();
                      // API 호출시간이 마지막 기사 발행시간보다 과거거나
                      // 같다면 재귀호출(네이버는 기사발행시간이 분단위로 절삭되어 비교해야 함)
                      if (lastCollectedAt.get(keyword).isBefore(lastPublishedAt)
                          || lastCollectedAt.get(keyword).equals(lastPublishedAt)) {
                        return fetchNewsData(webClient, source.getBaseUrl(), keyword, data.page() + 1)
                            .map(raw -> newsParseService.parse(NewsSourceType.NAVER, raw));
                      }

                      // API 호출시간이 기사 리스트 안쪽에 포함되어있다면, 즉 이미 봤다고 가정하면
                      lastCollectedAt.put(keyword, data.lastBuildDate());
                      return Mono.empty();
                    })
            , 5); // 동시성 제한

  }


  private Mono<RawArticleResult> fetchNewsData(
      WebClient webClient, String baseUrl, String keyword, int page) {

    return webClient.get().uri(builder ->
            URI.create(baseUrl +
                builder
                    .path(NAVER_QUERY_PATH)
                    .queryParam("query", keyword)
                    .queryParam("display", NAVER_QUERY_DISPLAY)
                    .queryParam("start", 100 * (page - 1) + 1)
                    .queryParam("sort", NAVER_QUERY_SORT)
                    .build())
        )
        .header("X-Naver-Client-Id", naverProperties.getId())
        .header("X-Naver-Client-Secret", naverProperties.getSecret())
        .retrieve()
        .bodyToMono(String.class)
        // 재시도 전략(최대 2번, 0.5초 간격)
        .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(500)))
        .map(rawData -> new RawArticleResult(rawData, keyword, page));
  }
}

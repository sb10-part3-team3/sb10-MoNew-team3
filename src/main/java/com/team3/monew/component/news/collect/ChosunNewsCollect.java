package com.team3.monew.component.news.collect;

import com.team3.monew.component.news.parse.NewsParser;
import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.RawArticleResult;
import com.team3.monew.entity.enums.NewsSourceType;
import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChosunNewsCollect implements NewsCollect {

  private final NewsParser newsParser;
  private final BasicErrorController basicErrorController;

  // Test를 위한 비 상수화
  private String chosunBaseUrl = "https://www.chosun.com";
  private static final String CHOSUN_QUERY_PATH = "/arc/outboundfeeds/rss/?outputType=xml";

  @Override
  public Flux<ParsedData> collect(WebClient webClient, NewsSourceType sourceType,
      Set<String> keywords) {
    String fullBaseUrl = chosunBaseUrl.contains("://") ? chosunBaseUrl : "http://" + chosunBaseUrl;
    String fullUrl = fullBaseUrl + CHOSUN_QUERY_PATH;

    return webClient.get().uri(fullUrl)
        .retrieve()
        // 400번대 에러
        .onStatus(HttpStatusCode::is4xxClientError, response ->
            // 바디를 읽지 않고 비움처리 후에 에러 전파
            response.releaseBody()
                .then(Mono.error(new RuntimeException("Chosun 요청 실패(4xx)")))
        )
        // 500번대 에러
        .onStatus(HttpStatusCode::is5xxServerError, response ->
            // 바디를 읽지 않고 비움처리 후에 에러 전파
            response.releaseBody()
                .then(Mono.error(new RuntimeException("Chosun 서버 일시적 장애(5xx)")))
        )
        .bodyToMono(String.class)
        // 재시도 전략(최대 2번, 0.5초 간격)
        .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(500))
            // 400번대 에러는 재시도 전략에서 제거
            .filter(throwable -> !(throwable instanceof RuntimeException && throwable.getMessage()
                .contains("4xx")))
        )
        .onErrorResume(e -> {
          log.error("Chosun 기사 수집 실패: error={}", e.getMessage());
          return Mono.empty();
        })
        .flatMap(rawData -> {
          log.info("Chosun 뉴스기사 rawData 받기 성공");
          ParsedData data = newsParser.parse(sourceType, new RawArticleResult(rawData, null, 0));

          if (data.isEmpty()) {
            log.warn("Chosun 뉴스기사 collect 실패");
            return Mono.empty();
          }

          log.info("Chosun 뉴스기사 collect 성공");
          return Mono.just(data);
        })
        .flux();
  }
}

package com.team3.monew.component.news.collect;

import com.team3.monew.component.news.parse.NewsParser;
import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.RawArticleResult;
import com.team3.monew.entity.enums.NewsSourceType;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChosunNewsCollect implements NewsCollect {

  private final NewsParser newsParser;
  private static final String CHOSUN_BASE_URL = "https://www.chosun.com";
  private static final String CHOSUN_QUERY_PATH = "/arc/outboundfeeds/rss/?outputType=xml";

  @Override
  public Flux<ParsedData> collect(WebClient webClient, NewsSourceType sourceType,
      Set<String> keywords) {
    return webClient.get().uri(CHOSUN_BASE_URL + CHOSUN_QUERY_PATH)
        .retrieve()
        .bodyToMono(String.class)
        .flatMap(rawData -> {
          log.info("Chosun 뉴스기사 rawData 받기 성공");
          ParsedData data = newsParser.parse(sourceType, new RawArticleResult(rawData, null, 0));
          log.info("Chosun 뉴스기사 collect 성공");
          return data.isEmpty() ? Mono.empty() : Mono.just(data);
        })
        .flux();
  }
}

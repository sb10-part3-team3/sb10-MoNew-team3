package com.team3.monew.service.news.collect;

import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.service.NewsParseService;
import com.team3.monew.service.NewsParseService.ParsedData;
import com.team3.monew.service.news.record.RawArticleResult;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class ChosunNewsCollect implements NewsCollect {

  private final NewsParseService newsParseService;
  private static final String CHOSUN_QUERY_PATH = "/arc/outboundfeeds/rss/?outputType=xml";

  @Override
  public Flux<ParsedData> collect(WebClient webClient, NewsSource source, Set<String> keywords) {
    return webClient.get().uri(source.getBaseUrl() + CHOSUN_QUERY_PATH)
        .retrieve()
        .bodyToMono(String.class)
        .map(rawData ->
            newsParseService.parse(NewsSourceType.CHOSUN, new RawArticleResult(rawData, null, 0)))
        .flux();
  }
}

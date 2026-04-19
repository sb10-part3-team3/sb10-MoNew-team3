package com.team3.monew.service;

import com.team3.monew.entity.NewsSource;
import com.team3.monew.service.NewsParseService.ParsedData;
import com.team3.monew.service.news.collect.NewsCollect;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCollectService {

  private final Map<String, NewsCollect> newsCollect;
  private final WebClient webClient = WebClient.builder()
      // 1개의 요청에 메모리 1MB 제한
      .codecs(config -> config.defaultCodecs().maxInMemorySize(1 * 1024 * 1024))
      .build();

  public Flux<ParsedData> collectRawNews(NewsSource source, Set<String> keywords) {
    String beanName = source.getSourceType().name().toLowerCase() + "NewsCollect";

    return newsCollect.get(beanName)
        .collect(webClient, source, keywords);
  }
}
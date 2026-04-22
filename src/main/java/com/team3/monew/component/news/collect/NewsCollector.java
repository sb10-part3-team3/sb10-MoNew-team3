package com.team3.monew.component.news.collect;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.exception.news.NewsIllegalBeanException;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollector {

  private final Map<String, NewsCollect> newsCollect;
  private final WebClient webClient = WebClient.builder()
      // 1개의 요청에 메모리 512KB 제한
      .codecs(config -> config.defaultCodecs().maxInMemorySize(512 * 1024))
      .build();

  public Flux<ParsedData> collectRawNews(NewsSourceType sourceType, Set<String> keywords) {
    String beanName = sourceType.name().toLowerCase() + "NewsCollect";
    NewsCollect collector = newsCollect.get(beanName);
    if (collector == null) {
      throw new NewsIllegalBeanException(Map.of("NewsCollect", beanName));
    }

    log.debug("NewsCollector: {} 시작", beanName);
    return collector.collect(webClient, sourceType, keywords);
  }
}

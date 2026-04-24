package com.team3.monew.component.news.collect;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.exception.news.NewsIllegalBeanException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

@Slf4j
@Component
public class NewsCollector {

  private final Map<String, NewsCollect> newsCollect;
  private final WebClient webClient;

  public NewsCollector(Map<String, NewsCollect> newsCollect) {
    this.newsCollect = newsCollect;

    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)                 // 연결 대기 3초
        .responseTimeout(Duration.ofSeconds(3))                     // 응답 대기 3초
        .doOnConnected(conn ->
            conn.addHandlerLast(new ReadTimeoutHandler(3))     // 읽기 대기 3초
                .addHandlerLast(new WriteTimeoutHandler(3)));  // 쓰기 대기 3초

    webClient = WebClient.builder()
        // 1개의 요청에 메모리 512KB 제한
        .codecs(config -> config.defaultCodecs().maxInMemorySize(512 * 1024))
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }


  public Flux<ParsedData> collectRawNews(NewsSourceType sourceType,
      Collection<InterestKeyword> interestKeywords) {
    String beanName = sourceType.name().toLowerCase() + "NewsCollect";
    NewsCollect collector = newsCollect.get(beanName);
    if (collector == null) {
      throw new NewsIllegalBeanException(Map.of("NewsCollect", beanName));
    }

    log.debug("NewsCollector: {} 시작", beanName);
    return collector.collect(webClient, sourceType, interestKeywords);
  }
}

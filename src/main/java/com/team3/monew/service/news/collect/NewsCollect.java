package com.team3.monew.service.news.collect;

import com.team3.monew.entity.NewsSource;
import com.team3.monew.service.NewsParseService.ParsedData;
import java.util.Set;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

public interface NewsCollect {

  Flux<ParsedData> collect(WebClient webClient, NewsSource source, Set<String> keywords);
}

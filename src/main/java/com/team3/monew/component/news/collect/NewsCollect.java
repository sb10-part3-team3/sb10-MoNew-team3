package com.team3.monew.component.news.collect;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.enums.NewsSourceType;
import java.util.Collection;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

public interface NewsCollect {

  Flux<ParsedData> collect(WebClient webClient, NewsSourceType sourceType,
      Collection<InterestKeyword> interestKeywords);
}

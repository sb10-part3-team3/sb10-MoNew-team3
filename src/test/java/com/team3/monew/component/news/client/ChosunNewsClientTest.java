package com.team3.monew.component.news.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team3.monew.component.news.collect.NewsCollector;
import com.team3.monew.component.news.filter.NewsFilter;
import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ChosunNewsClientTest {

  @Mock
  private NewsCollector newsCollector;
  @Mock
  private NewsFilter newsFilter;

  @InjectMocks
  private ChosunNewsClient chosunNewsClient;

  @Test
  @DisplayName("키워드가 없으면 빈 List를 반환한다")
  void shouldReturnEmptyList_whenKeywordsIsEmpty() {
    // given
    List<ParsedNewsArticle> articles = List.of();
    ParsedData parsedData = new ParsedData(null, null, 0, articles);

    given(newsCollector.collectRawNews(any(), any())).willReturn(Flux.just(parsedData));
    given(newsFilter.filterKeyword(any())).willReturn(articles);

    // when, then
    StepVerifier.create(chosunNewsClient.fetchAndProcess(Set.of()))
        .expectNext(articles)
        .verifyComplete();  // 스트림 정상종료 확인
  }

  @Test
  @DisplayName("키워드가 있어도 매칭되지 않으면 빈 List를 반환한다")
  void shouldReturnEmptyList_whenKeywordsAreUnmatched() {
    // given
    List<ParsedNewsArticle> articles = List.of();
    ParsedData parsedData = new ParsedData(null, null, 0, articles);

    given(newsCollector.collectRawNews(any(), any())).willReturn(Flux.just(parsedData));
    given(newsFilter.filterKeyword(any())).willReturn(articles);

    // when, then
    StepVerifier.create(chosunNewsClient.fetchAndProcess(Set.of("삼성")))
        .expectNext(articles)
        .verifyComplete();
  }

  @Test
  @DisplayName("키워드와 매칭되는 뉴스기사가 있으면 Raw기사 List를 반환한다")
  void shouldReturnRawArticleList_whenKeywordsAreMatched() {
    // given
    List<ParsedNewsArticle> articles = List.of(
        new ParsedNewsArticle(null, null, "삼성", null, null, List.of("삼성")),
        new ParsedNewsArticle(null, null, null, null, "삼성", List.of("삼성"))
    );
    ParsedData parsedData = new ParsedData(null, null, 0, articles);

    given(newsCollector.collectRawNews(any(), any())).willReturn(Flux.just(parsedData));
    given(newsFilter.filterKeyword(any())).willReturn(articles);

    // when, then
    StepVerifier.create(chosunNewsClient.fetchAndProcess(Set.of("삼성")))
        .expectNext(articles)
        .verifyComplete();
  }
}
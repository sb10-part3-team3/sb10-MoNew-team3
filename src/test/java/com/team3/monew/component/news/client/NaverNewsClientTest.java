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
class NaverNewsClientTest {

  @Mock
  private NewsCollector newsCollector;
  @Mock
  private NewsFilter newsFilter;

  @InjectMocks
  private NaverNewsClient naverNewsClient;

  @Test
  @DisplayName("키워드와 매칭되는 뉴스기사가 있으면 뉴스를 반환한다")
  void shouldReturnParsedNewsArticleList_whenKeywordsAreMatched() {
    // given
    List<ParsedNewsArticle> articles = List.of(
        new ParsedNewsArticle(null, "1", "삼성", null, null, List.of("삼성")),
        new ParsedNewsArticle(null, "2", null, null, "삼성", List.of("삼성"))
    );
    ParsedData parsedData = new ParsedData(null, null, 0, articles);

    given(newsCollector.collectRawNews(any(), any())).willReturn(Flux.just(parsedData));
    given(newsFilter.filterKeyword(any())).willReturn(articles);

    // when, then
    StepVerifier.create(naverNewsClient.fetchAndProcess(Set.of("삼성")))
        .expectNext(articles)
        .verifyComplete();
  }

  @Test
  @DisplayName("다중 키워드로 여러 기사 List를 받아도 하나의 기사 List로 만들어 반환한다")
  void shouldReturnParsedNewsArticleList_whenMultiKeywordsAreMatched() {
    // given
    List<ParsedNewsArticle> articles = List.of(
        new ParsedNewsArticle(null, "1", "삼성", null, null, List.of("삼성")),
        new ParsedNewsArticle(null, "2", null, null, "애플", List.of("애플"))
    );
    ParsedData parsedData = new ParsedData(null, null, 0, articles);

    given(newsCollector.collectRawNews(any(), any())).willReturn(Flux.just(parsedData));
    given(newsFilter.filterKeyword(any())).willReturn(articles);

    // when, then
    StepVerifier.create(naverNewsClient.fetchAndProcess(Set.of("삼성", "애플")))
        .expectNext(articles)
        .verifyComplete();
  }

  @Test
  @DisplayName("다중 키워드로 동일한 뉴스기사를 가져와도 중복제거를 한 기사 List를 반환한다")
  void shouldReturnParsedNewsArticleList_whenKeywordsIsEmpty() {
    // given
    List<ParsedNewsArticle> articles = List.of(
        new ParsedNewsArticle(null, "1", "삼성", null, "애플", List.of("삼성", "애플"))
    );
    ParsedData parsedData = new ParsedData(null, null, 0, articles);

    given(newsCollector.collectRawNews(any(), any())).willReturn(Flux.just(parsedData));
    given(newsFilter.filterKeyword(any())).willReturn(articles);

    // when, then
    StepVerifier.create(naverNewsClient.fetchAndProcess(Set.of("삼성", "애플")))
        .expectNext(articles)
        .verifyComplete();
  }

  @Test
  @DisplayName("키워드가 없으면 빈 List를 반환한다")
  void shouldReturnEmptyList_whenKeywordsIsEmpty() {
    // given
    List<ParsedNewsArticle> articles = List.of();
    ParsedData parsedData = new ParsedData(null, null, 0, articles);

    given(newsCollector.collectRawNews(any(), any())).willReturn(Flux.just(parsedData));
    given(newsFilter.filterKeyword(any())).willReturn(articles);

    // when, then
    StepVerifier.create(naverNewsClient.fetchAndProcess(Set.of()))
        .expectNext(articles)
        .verifyComplete();
  }
}

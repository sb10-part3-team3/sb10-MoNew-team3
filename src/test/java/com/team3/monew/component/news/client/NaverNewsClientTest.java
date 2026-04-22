package com.team3.monew.component.news.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team3.monew.component.news.collect.NewsCollector;
import com.team3.monew.component.news.filter.NewsFilter;
import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.enums.NewsSourceType;
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
    List<ParsedNewsArticle> articles1 = List.of(
        new ParsedNewsArticle(null, "1", "삼성", null, null, List.of("삼성")),
        new ParsedNewsArticle(null, "2", null, null, "애플", List.of("애플"))
    );
    ParsedData parsedData1 = new ParsedData(NewsSourceType.NAVER, null, 1, articles1);

    List<ParsedNewsArticle> articles2 = List.of(
        new ParsedNewsArticle(null, "1", "삼성", null, null, List.of("삼성"))
    );
    ParsedData parsedData2 = new ParsedData(NewsSourceType.NAVER, null, 0, articles2);

    given(newsCollector.collectRawNews(any(), any()))
        .willReturn(Flux.just(parsedData1))
        .willReturn(Flux.just(parsedData2));
    given(newsFilter.filterKeyword(any()))
        .willReturn(articles1)
        .willReturn(articles2);

    // when, then
    StepVerifier.create(naverNewsClient.fetchAndProcess(Set.of("삼성", "애플")))
        .assertNext(list -> {
          assertThat(list)
              .hasSize(2)
              .extracting("link")
              .containsExactly("1", "2");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("다중 키워드로 동일한 뉴스기사를 가져와도 중복제거를 한 기사 List를 반환한다")
  void shouldReturnDeduplicatedList_whenDuplicateArticlesExist() {
    // given
    List<ParsedNewsArticle> articles1 = List.of(
        new ParsedNewsArticle(null, "1", "삼성", null, "애플", List.of("삼성"))
    );
    ParsedData parsedData1 = new ParsedData(NewsSourceType.NAVER, null, 0, articles1);

    List<ParsedNewsArticle> articles2 = List.of(
        new ParsedNewsArticle(null, "1", "삼성", null, "애플", List.of("애플"))
    );
    ParsedData parsedData2 = new ParsedData(NewsSourceType.NAVER, null, 0, articles2);

    given(newsCollector.collectRawNews(any(), any()))
        .willReturn(Flux.just(parsedData1))
        .willReturn(Flux.just(parsedData2));
    given(newsFilter.filterKeyword(any()))
        .willReturn(articles1)
        .willReturn(articles2);

    // when, then
    StepVerifier.create(naverNewsClient.fetchAndProcess(Set.of("삼성", "애플")))
        .assertNext(list -> {
          assertThat(list)
              .hasSize(1)
              .extracting("link")
              .containsExactly("1");
        })
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

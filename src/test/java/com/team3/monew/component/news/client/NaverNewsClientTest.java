package com.team3.monew.component.news.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.team3.monew.component.news.collect.NewsCollector;
import com.team3.monew.component.news.filter.NewsFilter;
import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.enums.NewsSourceType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

  private Interest samsungInterest;
  private InterestKeyword samsungKeyword;
  private List<InterestKeyword> samsungKeywordList;

  private Interest appleInterest;
  private InterestKeyword appleKeyword;
  private List<InterestKeyword> appleKeywordList;

  private ParsedNewsArticle samsungArticle;
  private ParsedNewsArticle appleArticle;


  @BeforeEach
  void setUp() {
    samsungInterest = Interest.create("삼성");
    samsungKeyword = InterestKeyword.create(samsungInterest, "메모리");
    samsungKeywordList = new ArrayList<>(List.of(samsungKeyword));

    appleInterest = Interest.create("애플");
    appleKeyword = InterestKeyword.create(appleInterest, "아이폰");
    appleKeywordList = new ArrayList<>(List.of(appleKeyword));

    samsungArticle = new ParsedNewsArticle(NewsSourceType.NAVER, "link1", "삼성", null, "메모리",
        samsungKeywordList);
    appleArticle = new ParsedNewsArticle(NewsSourceType.NAVER, "link2", "애플", null, "아이폰",
        appleKeywordList);
  }

  @Test
  @DisplayName("키워드와 매칭되는 뉴스기사가 있으면 뉴스를 반환한다")
  void shouldReturnParsedNewsArticleList_whenKeywordsAreMatched() {
    // given
    List<ParsedNewsArticle> parsedNewsArticleList = List.of(samsungArticle);
    ParsedData parsedData = new ParsedData(NewsSourceType.NAVER, null, 1, parsedNewsArticleList);

    given(newsCollector.collectRawNews(any(), any())).willReturn(Flux.just(parsedData));
    given(newsFilter.filterKeyword(any(), anyList())).willReturn(parsedNewsArticleList);

    // when & then
    StepVerifier.create(naverNewsClient.fetchAndProcess(samsungKeywordList))
        .expectNext(parsedNewsArticleList)
        .verifyComplete();
  }

  @Test
  @DisplayName("다중 키워드로 여러 기사 List를 받아도 하나의 기사 List로 만들어 반환한다")
  void shouldReturnParsedNewsArticleList_whenMultiKeywordsAreMatched() {
    // given
    List<ParsedNewsArticle> articles1 = List.of(samsungArticle, appleArticle);
    ParsedData parsedData1 = new ParsedData(NewsSourceType.NAVER, null, 1, articles1);

    List<ParsedNewsArticle> articles2 = List.of(
        new ParsedNewsArticle(null, "link1", "삼성 애플", null, "메모리 아이폰", appleKeywordList)
    );
    ParsedData parsedData2 = new ParsedData(NewsSourceType.NAVER, null, 0, articles2);

    given(newsCollector.collectRawNews(any(), any()))
        .willReturn(Flux.just(parsedData1, parsedData2));
    given(newsFilter.filterKeyword(any(), anyList()))
        .willReturn(articles1)
        .willReturn(articles2);

    // when & then
    StepVerifier.create(naverNewsClient.fetchAndProcess(List.of(samsungKeyword, appleKeyword)))
        .assertNext(list -> {
          assertThat(list)
              .hasSize(2)
              .extracting("link")
              .containsExactly("link1", "link2");
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("다중 키워드로 동일한 뉴스기사를 가져와도 중복제거를 한 기사 List를 반환한다")
  void shouldReturnDeduplicatedList_whenDuplicateArticlesExist() {
    // given
    List<ParsedNewsArticle> articles1 = List.of(samsungArticle);
    ParsedData parsedData1 = new ParsedData(NewsSourceType.NAVER, null, 0, articles1);

    List<ParsedNewsArticle> articles2 = List.of(
        new ParsedNewsArticle(null, "link1", "삼성 애플", null, "메모리 아이폰", appleKeywordList)
    );
    ParsedData parsedData2 = new ParsedData(NewsSourceType.NAVER, null, 0, articles2);

    given(newsCollector.collectRawNews(any(), any()))
        .willReturn(Flux.just(parsedData1, parsedData2));
    given(newsFilter.filterKeyword(any(), anyList()))
        .willReturn(articles1)
        .willReturn(articles2);

    // when, then
    StepVerifier.create(naverNewsClient.fetchAndProcess(List.of(samsungKeyword, appleKeyword)))
        .assertNext(list -> {
          assertThat(list)
              .hasSize(1)
              .extracting("link")
              .containsExactly("link1");
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
    given(newsFilter.filterKeyword(any(), anyList())).willReturn(articles);

    // when, then
    StepVerifier.create(naverNewsClient.fetchAndProcess(List.of()))
        .expectNext(articles)
        .verifyComplete();
  }
}

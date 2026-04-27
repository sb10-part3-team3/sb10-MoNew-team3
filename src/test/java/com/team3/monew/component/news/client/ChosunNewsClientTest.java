package com.team3.monew.component.news.client;

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
class ChosunNewsClientTest {

  @Mock
  private NewsCollector newsCollector;
  @Mock
  private NewsFilter newsFilter;

  @InjectMocks
  private ChosunNewsClient chosunNewsClient;

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
  @DisplayName("키워드가 없으면 빈 List를 반환한다")
  void shouldReturnEmptyList_whenKeywordsIsEmpty() {
    // given
    List<ParsedNewsArticle> articles = List.of();
    ParsedData parsedData = new ParsedData(null, null, 0, articles);

    given(newsCollector.collectRawNews(any(), any())).willReturn(Flux.just(parsedData));
    given(newsFilter.filterKeyword(any(), anyList())).willReturn(articles);

    // when, then
    StepVerifier.create(chosunNewsClient.fetchAndProcess(List.of()))
        .expectNext(articles)
        .verifyComplete();  // 스트림 정상종료 확인
  }

  @Test
  @DisplayName("키워드와 매칭되는 뉴스기사가 있으면 Raw기사 List를 반환한다")
  void shouldReturnRawArticleList_whenKeywordsAreMatched() {
    // given
    List<ParsedNewsArticle> articles = List.of(samsungArticle);
    ParsedData parsedData = new ParsedData(null, null, 0, articles);

    given(newsCollector.collectRawNews(any(), any())).willReturn(Flux.just(parsedData));
    given(newsFilter.filterKeyword(any(), anyList())).willReturn(articles);

    // when, then
    StepVerifier.create(chosunNewsClient.fetchAndProcess(List.of(samsungKeyword)))
        .expectNext(articles)
        .verifyComplete();
  }
}

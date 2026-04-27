package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.timeout;

import com.team3.monew.component.news.client.ChosunNewsClient;
import com.team3.monew.component.news.client.NaverNewsClient;
import com.team3.monew.component.news.client.NewsClient;
import com.team3.monew.component.news.filter.KeywordMatch;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.InterestKeywordRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NewsCollectServiceTest {

  @Mock
  private InterestKeywordRepository interestKeywordRepository;
  @Mock
  private KeywordMatch keywordMatch;
  @Mock
  private Map<String, NewsClient> newsClients;
  @Mock
  private NaverNewsClient naverNewsClient;
  @Mock
  private ChosunNewsClient chosunNewsClient;
  @Mock
  private NewsSaveService newsSaveService;

  @InjectMocks
  private NewsCollectService newsCollectService;

  @Captor
  private ArgumentCaptor<List<ParsedNewsArticle>> listCaptor;

  List<InterestKeyword> interestKeywords;
  List<NewsSource> newsSources;

  private Interest samsungInterest;
  private Interest appleInterest;

  @BeforeEach
  void setUp() {
    appleInterest = Interest.create("애플");
    samsungInterest = Interest.create("삼성");

    interestKeywords = List.of(
        InterestKeyword.create(appleInterest, "메모리"),
        InterestKeyword.create(samsungInterest, "메모리"),
        InterestKeyword.create(samsungInterest, "화성공장")
    );

    NewsSource naver = NewsSource.create("NAVER", NewsSourceType.NAVER, "baseUrl1");
    NewsSource chosun = NewsSource.create("CHOSUN", NewsSourceType.CHOSUN, "baseUrl2");
    newsSources = List.of(naver, chosun);
  }

  @Test
  @DisplayName("동일한 링크 기사가 여러 Source에서 수집되면, 네이버 기사를 최우선으로 남긴다")
  void shouldKeepNaverArticle_whenMultipleSourcesProvideSameLink() {
    // given
    given(interestKeywordRepository.findAllWithInterest()).willReturn(interestKeywords);
    given(newsClients.values()).willReturn(List.of(naverNewsClient, chosunNewsClient));
    given(newsClients.size()).willReturn(2);

    String commonLink = "commonLink";
    List<ParsedNewsArticle> naverList = List.of(
        new ParsedNewsArticle(NewsSourceType.NAVER, commonLink, "제목1", Instant.now(), null,
            List.of(InterestKeyword.create(samsungInterest, "메모리")))
    );
    given(naverNewsClient.fetchAndProcess(interestKeywords)).willReturn(Mono.just(naverList));
    List<ParsedNewsArticle> chosunList = List.of(
        new ParsedNewsArticle(NewsSourceType.CHOSUN, commonLink, "제목1", Instant.now(), null,
            List.of(InterestKeyword.create(samsungInterest, "메모리"))),
        new ParsedNewsArticle(NewsSourceType.CHOSUN, "another Link2", "제목2", Instant.now(), null,
            List.of(InterestKeyword.create(appleInterest, "메모리")))
    );
    given(chosunNewsClient.fetchAndProcess(interestKeywords)).willReturn(Mono.just(chosunList));

    // when
    newsCollectService.scheduleNewsJob();

    // then
    // 비동기 테스트 타이밍 문제로 1초 유예기간 부여
    then(newsSaveService).should(timeout(1000)).saveAndNotify(listCaptor.capture());
    List<ParsedNewsArticle> deduplicatedLink = listCaptor.getValue();
    assertThat(deduplicatedLink)
        .hasSize(2)
        .filteredOn(l -> Objects.equals(l.link(), commonLink))
        .extracting(ParsedNewsArticle::sourceType)
        .containsExactly(NewsSourceType.NAVER);
  }
}

package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.event.InterestNotificationEvent;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NewsSaveServiceTest {

  @Mock
  private NewsArticleRepository newsArticleRepository;
  @Mock
  private NewsSourceRepository newsSourceRepository;
  @Mock
  private InterestRepository interestRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private NewsSaveService newsSaveService;

  @Captor
  ArgumentCaptor<List<NewsArticle>> listCaptor;

  @Test
  @DisplayName("중복된 링크를 제거한 새로운 뉴스기사를 저장한다")
  void shouldSaveAndNotifyOnlyNewArticles_whenDuplicateLinksExist() {
    // given
    Interest samsungInterest = Interest.create("삼성");
    List<InterestKeyword> samsungKeywordList = List.of(
        InterestKeyword.create(samsungInterest, "메모리"),
        InterestKeyword.create(samsungInterest, "갤럭시")
    );
    Instant articleTime1 = Instant.now();
    Instant articleTime2 = Instant.now();

    List<ParsedNewsArticle> givenData = List.of(
        new ParsedNewsArticle(NewsSourceType.NAVER, "link1", "기사1", articleTime1, null,
            samsungKeywordList),
        new ParsedNewsArticle(NewsSourceType.CHOSUN, "link2", "기사2", articleTime2, null,
            samsungKeywordList),
        new ParsedNewsArticle(NewsSourceType.NAVER, "link3", "기사3", null, null,
            samsungKeywordList)
    );
    String existingLink = "link3";
    given(newsArticleRepository.findExistingOriginalLinks(any(), anyList()))
        .willReturn(Set.of(existingLink));

    NewsSource naver = NewsSource.create("NAVER", NewsSourceType.NAVER, "baseUrl1");
    NewsSource chosun = NewsSource.create("CHOSUN", NewsSourceType.CHOSUN, "baseUrl2");
    given(newsSourceRepository.findAll()).willReturn(List.of(naver, chosun));

    given(interestRepository.findAll()).willReturn(List.of(samsungInterest));

    List<NewsArticle> expectData = List.of(
        NewsArticle.create(naver, "link1", "기사1", articleTime1, null),
        NewsArticle.create(chosun, "link2", "기사2", articleTime2, null)
    );

    // when
    List<NewsArticle> actualData = newsSaveService.saveAndNotify(givenData);

    // then
    then(newsArticleRepository).should().saveAll(listCaptor.capture());
    assertThat(listCaptor.getValue())
        .hasSize(2)
        .extracting(NewsArticle::getOriginalLink)
        .containsExactly("link1", "link2");
    assertThat(actualData)
        .hasSize(2)
        .extracting(NewsArticle::getOriginalLink)
        .containsExactly("link1", "link2");
    then(eventPublisher).should(times(1))
        .publishEvent(any(InterestNotificationEvent.class));
  }

  @Test
  @DisplayName("빈 데이터가 오면 빈 List를 반환한다")
  void shouldReturnEmptyList_whenInputDataIsEmpty() {
    // when
    List<NewsArticle> actualData = newsSaveService.saveAndNotify(List.of());

    // then
    assertThat(actualData)
        .isEmpty();
  }

  @Test
  @DisplayName("모든 기사가 이미 존재하면 빈 List를 반환한다")
  void shouldReturnEmptyList_whenAllArticlesAlreadyExist() {
    // given
    List<InterestKeyword> samsungKeywordList = List.of(
        InterestKeyword.create(Interest.create("삼성"), "메모리"));
    List<ParsedNewsArticle> data = List.of(
        new ParsedNewsArticle(NewsSourceType.NAVER, "Link1", "삼성", null, "메모리", samsungKeywordList),
        new ParsedNewsArticle(NewsSourceType.NAVER, "Link2", "삼성 파업", null, "메모리 노동자..",
            samsungKeywordList)
    );
    given(newsArticleRepository.findExistingOriginalLinks(any(), anyList()))
        .willReturn(Set.of("Link1", "Link2"));

    // when
    List<NewsArticle> actualData = newsSaveService.saveAndNotify(data);

    // then
    assertThat(actualData)
        .isEmpty();
  }
}

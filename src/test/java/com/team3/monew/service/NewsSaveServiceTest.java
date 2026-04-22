package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NewsSaveServiceTest {

  @Mock
  private NewsArticleRepository newsArticleRepository;
  @Mock
  private NewsSourceRepository newsSourceRepository;

  @InjectMocks
  private NewsSaveService newsSaveService;

  @Test
  @DisplayName("중복된 링크를 제거한 새로운 뉴스기사를 저장한다")
  void shouldSaveOnlyNewArticles_whenDuplicateLinksExist() {
    // given
    List<ParsedNewsArticle> givenData = List.of(
        new ParsedNewsArticle(NewsSourceType.NAVER, "link1", "기사1", Instant.now(), null,
            List.of("삼성")),
        new ParsedNewsArticle(NewsSourceType.CHOSUN, "link2", "기사2", Instant.now(), null,
            List.of("삼성")),
        new ParsedNewsArticle(NewsSourceType.NAVER, "link3", "기사3", Instant.now(), null,
            List.of("삼성"))
    );
    NewsArticle existingNews = NewsArticle.create(null, "link3", "기사3", null, null);
    given(newsArticleRepository.findAllByOriginalLinkIn(anyList()))
        .willReturn(List.of(existingNews));

    NewsSource naver = NewsSource.create("NAVER", NewsSourceType.NAVER, "baseUrl1");
    NewsSource chosun = NewsSource.create("CHOSUN", NewsSourceType.CHOSUN, "baseUrl2");
    given(newsSourceRepository.findAll()).willReturn(List.of(naver, chosun));

    // when
    List<ParsedNewsArticle> actualData = newsSaveService.save(givenData);

    // then
    assertThat(actualData)
        .hasSize(2)
        .extracting(ParsedNewsArticle::link)
        .containsExactly("link1", "link2");
  }
}

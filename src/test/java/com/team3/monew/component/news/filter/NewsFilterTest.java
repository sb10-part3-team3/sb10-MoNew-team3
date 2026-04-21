package com.team3.monew.component.news.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.service.KeywordMatchService;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NewsFilterTest {

  @Mock
  private KeywordMatchService keywordMatchService;

  @InjectMocks
  private NewsFilter newsFilter;

  @Test
  @DisplayName("키워드에 일치한 기사들만 반환한다")
  void shouldReturnFilteredArticles_whenKeywordsMatch() {
    // given
    List<ParsedNewsArticle> newsArticles = List.of(
        new ParsedNewsArticle(null, null, "삼성 만세", null, " HBM 메모리..", new ArrayList<>()),
        new ParsedNewsArticle(null, null, "애플", null, "폴더블", new ArrayList<>())
    );
    ParsedData parsedData = new ParsedData(NewsSourceType.NAVER, null, 0, newsArticles);

    given(keywordMatchService.findMatches(any()))
        .willReturn(List.of("삼성"))
        .willReturn(List.of("메모리"))
        .willReturn(List.of())
        .willReturn(List.of());

    // when
    List<ParsedNewsArticle> actualData = newsFilter.filterKeyword(parsedData);

    // then
    assertThat(actualData)
        .hasSize(1)
        .first()
        .extracting(ParsedNewsArticle::keywords)
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .contains("삼성", "메모리");
  }
}
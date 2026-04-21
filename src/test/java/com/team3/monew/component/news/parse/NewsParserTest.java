package com.team3.monew.component.news.parse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.RawArticleResult;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.exception.news.NewsIllegalBeanException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NewsParserTest {

  @Mock
  private Map<String, NewsParse> newsParse;
  @Mock
  private NaverNewsParse naverNewsParse;

  @InjectMocks
  private NewsParser newsParser;

  @Test
  @DisplayName("없는 bean을 요청하면 Exception 반환한다")
  void shouldThrowException_whenInvalidBeanName() {
    // given
    given(newsParse.get(anyString())).willReturn(null);

    // when, then
    assertThatThrownBy(() ->
        newsParser.parse(NewsSourceType.NAVER, null))
        .isInstanceOf(NewsIllegalBeanException.class);
  }

  @Test
  @DisplayName("적합한 bean을 꺼내 함수를 호출하고 parsing된 데이터를 반환한다")
  void shouldReturnParsedData_whenValidSourceTypeIsProvided() {
    // given
    given(newsParse.get(anyString())).willReturn(naverNewsParse);

    ParsedData parsedData = new ParsedData(null, null, 1, List.of());
    given(naverNewsParse.parse(any(), any())).willReturn(parsedData);

    RawArticleResult mockArticle = new RawArticleResult(null, null, 0);

    // when
    newsParser.parse(NewsSourceType.NAVER, mockArticle);

    // when, then
    then(naverNewsParse).should().parse(eq(NewsSourceType.NAVER), any());
  }
}
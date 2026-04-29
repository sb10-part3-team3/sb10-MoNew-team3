package com.team3.monew.component.news.collect;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team3.monew.component.news.record.ParsedData;
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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NewsCollectorTest {

  @Mock
  private Map<String, NewsCollect> newsCollect;
  @Mock
  private NaverNewsCollect naverNewsCollect;

  @InjectMocks
  private NewsCollector newsCollector;

  @Test
  @DisplayName("없는 bean을 요청하면 Exception 반환한다")
  void shouldThrowException_whenInvalidBeanName() {
    // given
    given(newsCollect.get(anyString())).willReturn(null);

    // when, then
    assertThatThrownBy(() ->
        newsCollector.collectRawNews(NewsSourceType.NAVER, List.of()))
        .isInstanceOf(NewsIllegalBeanException.class);
  }

  @Test
  @DisplayName("적합한 bean을 꺼내 함수를 호출하고 data를 반환한다")
  void shouldReturnParsedData_whenValidSourceTypeIsProvided() {
    // given
    given(newsCollect.get(anyString())).willReturn(naverNewsCollect);

    ParsedData parsedData = new ParsedData(null, null, 1, List.of());
    given(naverNewsCollect.collect(any(), any(), anyList())).willReturn(
        Flux.just(parsedData));

    // when, then
    StepVerifier.create(newsCollector.collectRawNews(NewsSourceType.NAVER, List.of()))
        .expectNext(parsedData)
        .verifyComplete();
    then(naverNewsCollect).should().collect(any(), eq(NewsSourceType.NAVER), any());
  }
}

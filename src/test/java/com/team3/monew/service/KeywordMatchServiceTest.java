package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class KeywordMatchServiceTest {

  @InjectMocks
  private KeywordMatchService keywordMatchService;

  @Test
  @DisplayName("주입받은 키워드가 갖고있는 키워드와 동일해도 그대로 작동한다")
  void shouldMaintainExistingPattern_whenSameKeywordsAreProvided() {
    // when
    String context = "삼성 메모리 반도체 HBM4E Nvidia..";
    keywordMatchService.refreshKeywords(Set.of("삼성", "메모리"));
    List<String> matched1 = keywordMatchService.findMatches(context);
    keywordMatchService.refreshKeywords(Set.of("메모리", "삼성"));
    List<String> matched2 = keywordMatchService.findMatches(context);

    // then
    assertThat(matched2)
        .isEqualTo(matched1)      // 내용물 같은지 검사
        .isNotSameAs(matched1);   // 객체 주소 다른지 검사
  }

  @Test
  @DisplayName("다른 키워드를 주입받으면 패턴이 갱신된다")
  void shouldUpdatePattern_whenDifferentKeywordsAreProvided() {
    // when
    String context = "삼성 메모리 반도체 HBM4E Nvidia..";
    keywordMatchService.refreshKeywords(Set.of("삼성", "메모리"));
    List<String> matched1 = keywordMatchService.findMatches(context);
    keywordMatchService.refreshKeywords(Set.of("반도체", "H..E"));
    List<String> matched2 = keywordMatchService.findMatches(context);

    // then
    assertThat(matched2)
        .isNotEqualTo(matched1)
        .contains("반도체");
  }

  @Test
  @DisplayName("대소문자가 달라도 매칭된 키워드를 반환한다")
  void shouldReturnMatchedKeywords_whenCaseIsDifferent() {
    // when
    String context = "Apple BANANA";
    keywordMatchService.refreshKeywords(Set.of("APPLE", "BanaNa"));
    List<String> matched = keywordMatchService.findMatches(context);

    // then
    assertThat(matched)
        .isEqualTo(Arrays.stream(context.split(" ")).toList());
  }

  @Test
  @DisplayName("주입받은 키워드가 없으면 매칭기능이 작동되지 않는다")
  void shouldReturnEmptyList_whenKeywordsAreEmpty() {
    // given
    keywordMatchService.refreshKeywords(Set.of());

    // when
    List<String> matchedKeywords = keywordMatchService.findMatches("메모리 삼성");

    // then
    assertThat(matchedKeywords)
        .hasSize(0);
  }
}
package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsSourceRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ArticleSourceServiceTest {

  @Mock
  private NewsSourceRepository newsSourceRepository;

  @InjectMocks
  private ArticleSourceService articleSourceService;

  @Nested
  @DisplayName("뉴스 기사 출처 목록을 조회한다")
  class GetArticleSources {

    @Test
    @DisplayName("등록된 출처가 있으면 중복 없이 문자열 목록을 반환한다")
    void shouldReturnDistinctArticleSources_whenNewsSourcesExist() {
      // given
      NewsSource naverPrimary = NewsSource.create("NAVER", NewsSourceType.NAVER,
          "https://openapi.naver.com");
      NewsSource naverSecondary = NewsSource.create("NAVER-SECONDARY", NewsSourceType.NAVER,
          "https://openapi.naver.com/news");
      NewsSource chosun = NewsSource.create("CHOSUN", NewsSourceType.CHOSUN,
          "https://www.chosun.com");

      given(newsSourceRepository.findAll()).willReturn(List.of(naverPrimary, naverSecondary, chosun));

      // when
      List<String> actual = articleSourceService.getArticleSources();

      // then
      assertThat(actual).containsExactly("NAVER", "CHOSUN");
    }

    @Test
    @DisplayName("등록된 출처가 없으면 빈 목록을 반환한다")
    void shouldReturnEmptyList_whenNewsSourcesDoNotExist() {
      // given
      given(newsSourceRepository.findAll()).willReturn(List.of());

      // when
      List<String> actual = articleSourceService.getArticleSources();

      // then
      assertThat(actual).isEmpty();
    }
  }
}

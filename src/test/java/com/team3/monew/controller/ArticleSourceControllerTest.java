package com.team3.monew.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.monew.global.exception.GlobalExceptionHandler;
import com.team3.monew.service.ArticleSourceService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Tag("unit")
@WebMvcTest(ArticleSourceController.class)
@Import(GlobalExceptionHandler.class)
class ArticleSourceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ArticleSourceService articleSourceService;

  @Nested
  @DisplayName("뉴스 기사 출처 목록 조회 API를 검증한다.")
  class GetArticleSources {

    @Test
    @DisplayName("등록된 출처가 있으면 문자열 배열을 반환한다.")
    void shouldReturnArticleSources_whenSourcesExist() throws Exception {
      // given
      given(articleSourceService.getArticleSources()).willReturn(List.of("NAVER", "CHOSUN"));

      // when & then
      mockMvc.perform(get("/api/articles/sources"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0]").value("NAVER"))
          .andExpect(jsonPath("$[1]").value("CHOSUN"));
    }

    @Test
    @DisplayName("등록된 출처가 없으면 빈 배열을 반환한다.")
    void shouldReturnEmptyArray_whenSourcesDoNotExist() throws Exception {
      // given
      given(articleSourceService.getArticleSources()).willReturn(List.of());

      // when & then
      mockMvc.perform(get("/api/articles/sources"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$").isEmpty());
    }
  }
}

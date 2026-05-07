package com.team3.monew.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.monew.service.ArticleService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Tag("unit")
@WebMvcTest(ArticleController.class)
class ArticleSourceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ArticleService articleService;

  @Nested
  @DisplayName("뉴스 기사 출처 목록 조회 API를 검증한다")
  class GetArticleSources {

    @Test
    @DisplayName("등록된 출처가 있으면 문자열 배열을 반환한다")
    void shouldReturnArticleSources_whenSourcesExist() throws Exception {
      given(articleService.getArticleSources()).willReturn(List.of("NAVER", "CHOSUN"));

      mockMvc.perform(get("/api/articles/sources"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0]").value("NAVER"))
          .andExpect(jsonPath("$[1]").value("CHOSUN"));
    }

    @Test
    @DisplayName("등록된 출처가 없으면 빈 배열을 반환한다")
    void shouldReturnEmptyArray_whenSourcesDoNotExist() throws Exception {
      given(articleService.getArticleSources()).willReturn(List.of());

      mockMvc.perform(get("/api/articles/sources"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$").isEmpty());
    }
  }
}

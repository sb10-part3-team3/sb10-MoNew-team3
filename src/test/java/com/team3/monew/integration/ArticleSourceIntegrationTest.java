package com.team3.monew.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.monew.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@Tag("integration")
class ArticleSourceIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("뉴스 기사 출처 목록 조회 요청이 성공하면 문자열 배열을 반환한다")
  void shouldReturnArticleSources_whenRequestIsValid() throws Exception {
    // when & then
    mockMvc.perform(get("/api/articles/sources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("NAVER"))
        .andExpect(jsonPath("$[1]").value("CHOSUN"));
  }
}

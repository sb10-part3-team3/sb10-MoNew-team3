package com.team3.monew.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.monew.dto.article.ArticleDto;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.service.ArticleService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class ArticleControllerTest {

  @MockitoBean
  private ArticleService articleService;

  @Autowired
  private MockMvc mockMvc;

  private static final String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";
  private static final String ARTICLES_BASE_URL = "/api/articles";

  private ArticleDto articleDto;

  @BeforeEach
  void setUp() {
    articleDto = new ArticleDto(UUID.randomUUID(), NewsSourceType.NAVER, "link1", "삼성 메모리..",
        Instant.now(), "메모리.. Nvidia 납품..", 5, 37, false);
  }

  @Nested
  class GetArticleList {

    @Test
    @DisplayName("유효한 파라미터를 받으면 커서페이지를 반환한다")
    void shouldGetCursorArticlePage_whenParamsAreValid() throws Exception {
      // given
      CursorPageResponseDto<ArticleDto> cursorPage = new CursorPageResponseDto<>(
          List.of(articleDto), null, null, 10, 1L, false
      );
      given(articleService.getArticleList(any())).willReturn(cursorPage);

      // when & then
      mockMvc.perform(get(ARTICLES_BASE_URL)
              .header(REQUEST_USER_ID_HEADER, UUID.randomUUID())
              .param("orderBy", "publishDate")
              .param("direction", "DESC")
              .param("limit", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.size()").value(1))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("헤더에 요청자ID가 없으면 오류를 반환한다")
    void shouldReturnError_whenRequestUserIdHeaderIsMissing() throws Exception {
      // when & then
      mockMvc.perform(get(ARTICLES_BASE_URL)
              .param("orderBy", "publishDate")
              .param("direction", "DESC")
              .param("limit", "10"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.details.header").value("Monew-Request-User-ID"));
    }

    @Test
    @DisplayName("유효하지 않은 파라미터를 받으면 오류를 반환한다")
    void shouldReturnError_whenParamsAreInvalid() throws Exception {
      // given
      String inputOrderBy = "publishDate2";
      String inputDirection = "DESC4";

      // when & then
      mockMvc.perform(get(ARTICLES_BASE_URL)
              .header(REQUEST_USER_ID_HEADER, UUID.randomUUID())
              .param("orderBy", inputOrderBy)
              .param("direction", inputDirection)
              .param("limit", "1"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
          .andExpect(jsonPath("$.details.orderBy").value(inputOrderBy))
          .andExpect(jsonPath("$.details.direction").value(inputDirection));
    }

    @Test
    @DisplayName("orderBy, direction 파라미터는 대소문자를 무시하고 정상처리된다")
    void shouldProcessSuccessfully_whenOrderByAndDirectionAreMixedCase() throws Exception {
      // when & then
      mockMvc.perform(get(ARTICLES_BASE_URL)
              .header(REQUEST_USER_ID_HEADER, UUID.randomUUID())
              .param("orderBy", "PublishDatE")
              .param("direction", "DesC")
              .param("limit", "1"))
          .andExpect(status().isOk());
    }
  }
}

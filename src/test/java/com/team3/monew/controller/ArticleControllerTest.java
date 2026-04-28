package com.team3.monew.controller;

import com.team3.monew.dto.article.ArticleViewDto;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.global.exception.GlobalExceptionHandler;
import com.team3.monew.service.ArticleService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@WebMvcTest(ArticleController.class)
@Import(GlobalExceptionHandler.class)
class ArticleControllerTest {

  private static final String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ArticleService articleService;

  @Nested
  @DisplayName("기사 뷰 등록 API를 검증한다")
  class RegisterArticleView {

    @Test
    @DisplayName("유효한 요청을 받으면 기사 뷰를 등록하고 조회 이력 정보를 반환한다.")
    void shouldRegisterArticleView_whenRequestIsValid() throws Exception {
      // given
      UUID articleViewId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      Instant viewedAt = Instant.parse("2026-04-28T10:15:30Z");

      ArticleViewDto response = new ArticleViewDto(
          articleViewId,
          requestUserId,
          viewedAt,
          articleId,
          "NAVER",
          "https://news.naver.com/article/1",
          "테스트 기사",
          Instant.parse("2026-04-28T09:00:00Z"),
          "테스트 기사 요약",
          3L,
          11L
      );

      given(articleService.registerArticleView(articleId, requestUserId)).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
              .header(REQUEST_USER_ID_HEADER, requestUserId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(articleViewId.toString()))
          .andExpect(jsonPath("$.viewedBy").value(requestUserId.toString()))
          .andExpect(jsonPath("$.createdAt").value("2026-04-28T10:15:30Z"))
          .andExpect(jsonPath("$.articleId").value(articleId.toString()))
          .andExpect(jsonPath("$.source").value("NAVER"))
          .andExpect(jsonPath("$.sourceUrl").value("https://news.naver.com/article/1"))
          .andExpect(jsonPath("$.articleTitle").value("테스트 기사"))
          .andExpect(jsonPath("$.articlePublishedDate").value("2026-04-28T09:00:00Z"))
          .andExpect(jsonPath("$.articleSummary").value("테스트 기사 요약"))
          .andExpect(jsonPath("$.articleCommentCount").value(3))
          .andExpect(jsonPath("$.articleViewCount").value(11));

      then(articleService).should().registerArticleView(articleId, requestUserId);
      then(articleService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("요청자 ID 헤더가 없으면 기사 뷰 등록에 실패하고 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenRequestUserIdHeaderIsMissing() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();

      // when & then
      mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.details.header").value(REQUEST_USER_ID_HEADER));

      then(articleService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 기사를 조회하면 404 Not Found로 응답한다.")
    void shouldReturnNotFound_whenArticleDoesNotExist() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      given(articleService.registerArticleView(articleId, requestUserId))
          .willThrow(new ArticleNotFoundException(articleId));

      // when & then
      mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
              .header(REQUEST_USER_ID_HEADER, requestUserId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("ARTICLE_NOT_FOUND"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.details.articleId").value(articleId.toString()));

      then(articleService).should().registerArticleView(articleId, requestUserId);
      then(articleService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("삭제된 기사를 조회하면 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenArticleIsDeleted() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      given(articleService.registerArticleView(articleId, requestUserId))
          .willThrow(new DeletedArticleException(articleId));

      // when & then
      mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
              .header(REQUEST_USER_ID_HEADER, requestUserId))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("ARTICLE_DELETED"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.details.articleId").value(articleId.toString()));

      then(articleService).should().registerArticleView(articleId, requestUserId);
      then(articleService).shouldHaveNoMoreInteractions();
    }
  }
}

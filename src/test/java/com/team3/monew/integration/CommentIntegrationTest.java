package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsArticleRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Transactional
class CommentIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  @Test
  @DisplayName("유효한 요청이면 댓글 등록에 성공하고 댓글과 기사 댓글 수를 저장한다.")
  void shouldRegisterComment_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User user = saveUser();
    CommentRegisterRequest request = new CommentRegisterRequest(
        article.getId(),
        "댓글 내용입니다."
    );

    // when & then
    mockMvc.perform(post("/api/comments")
            .principal(() -> user.getId().toString())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.articleId").value(article.getId().toString()))
        .andExpect(jsonPath("$.userId").value(user.getId().toString()))
        .andExpect(jsonPath("$.userNickname").value(user.getNickname()))
        .andExpect(jsonPath("$.content").value("댓글 내용입니다."))
        .andExpect(jsonPath("$.likeCount").value(0))
        .andExpect(jsonPath("$.likedByMe").value(false));

    entityManager.flush();
    entityManager.clear();

    Long commentCount = entityManager.createQuery(
            "select count(c) from Comment c where c.article.id = :articleId",
            Long.class
        )
        .setParameter("articleId", article.getId())
        .getSingleResult();
    NewsArticle savedArticle = newsArticleRepository.findById(article.getId()).orElseThrow();

    assertThat(commentCount).isEqualTo(1);
    assertThat(savedArticle.getCommentCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("존재하지 않는 기사에 댓글을 등록하면 404 Not Found로 응답한다.")
  void shouldReturnNotFound_whenArticleDoesNotExist() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    CommentRegisterRequest request = new CommentRegisterRequest(
        UUID.randomUUID(),
        "댓글 내용입니다."
    );

    // when & then
    mockMvc.perform(post("/api/comments")
            .principal(() -> userId.toString())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ARTICLE_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.details.articleId").value(request.articleId().toString()));
  }

  @Test
  @DisplayName("content가 비어 있으면 댓글 등록에 실패하고 400 Bad Request로 응답한다.")
  void shouldReturnBadRequest_whenContentIsEmpty() throws Exception {
    // given
    CommentRegisterRequest request = new CommentRegisterRequest(
        UUID.randomUUID(),
        ""
    );

    // when & then
    mockMvc.perform(post("/api/comments")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.content").exists());
  }

  private NewsArticle saveArticle() {
    UUID id = UUID.randomUUID();
    NewsSource source = NewsSource.create(
        "source-" + id,
        NewsSourceType.NAVER,
        "https://news.example.com"
    );
    NewsArticle article = NewsArticle.create(
        source,
        "https://news.example.com/articles/" + id,
        "테스트 기사",
        Instant.parse("2026-04-17T00:00:00Z"),
        "테스트 기사 요약"
    );

    entityManager.persist(source);
    entityManager.persist(article);
    entityManager.flush();
    entityManager.clear();
    return article;
  }

  private User saveUser() {
    UUID id = UUID.randomUUID();
    User user = User.create(
        "user-" + id + "@example.com",
        "테스터",
        "encoded-password"
    );

    entityManager.persist(user);
    entityManager.flush();
    entityManager.clear();
    return user;
  }
}

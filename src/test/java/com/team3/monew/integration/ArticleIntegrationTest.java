package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.entity.ArticleView;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
@Tag("integration")
class ArticleIntegrationTest extends IntegrationTestSupport {

  private static final String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private EntityManager entityManager;

  @Test
  @DisplayName("기사 뷰 등록 요청이 성공하면 조회 이력이 생성되고 기사 조회 수가 증가한다")
  void shouldRegisterArticleView_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User user = saveUser();

    // when & then
    mockMvc.perform(post("/api/articles/{articleId}/article-views", article.getId())
            .header(REQUEST_USER_ID_HEADER, user.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.viewedBy").value(user.getId().toString()))
        .andExpect(jsonPath("$.articleId").value(article.getId().toString()))
        .andExpect(jsonPath("$.source").value(findSourceName(article.getId())))
        .andExpect(jsonPath("$.sourceUrl").value(article.getOriginalLink()))
        .andExpect(jsonPath("$.articleTitle").value(article.getTitle()))
        .andExpect(jsonPath("$.articlePublishedDate").value(article.getPublishedAt().toString()))
        .andExpect(jsonPath("$.articleSummary").value(article.getSummary()))
        .andExpect(jsonPath("$.articleCommentCount").value(0))
        .andExpect(jsonPath("$.articleViewCount").value(1));

    entityManager.flush();
    entityManager.clear();

    assertThat(countArticleViews(article, user)).isEqualTo(1);
    assertThat(findArticleViewCount(article)).isEqualTo(1);
  }

  @Test
  @DisplayName("같은 사용자가 같은 기사를 다시 조회하면 조회 이력은 재사용되고 기사 조회 수는 증가하지 않는다")
  void shouldReuseExistingArticleViewWithoutIncreasingViewCount_whenArticleViewedAgain()
      throws Exception {
    // given
    NewsArticle article = saveArticle();
    User user = saveUser();

    JsonNode firstResponse = registerArticleView(article.getId(), user.getId());
    UUID articleViewId = UUID.fromString(firstResponse.get("id").asText());
    Instant firstLastViewedAt = findLastViewedAt(articleViewId);

    Thread.sleep(10L);

    // when
    JsonNode secondResponse = registerArticleView(article.getId(), user.getId());

    // then
    entityManager.flush();
    entityManager.clear();

    assertThat(UUID.fromString(secondResponse.get("id").asText())).isEqualTo(articleViewId);
    assertThat(secondResponse.get("articleViewCount").asInt()).isEqualTo(1);
    assertThat(countArticleViews(article, user)).isEqualTo(1);
    assertThat(findArticleViewCount(article)).isEqualTo(1);
    assertThat(findLastViewedAt(articleViewId)).isAfter(firstLastViewedAt);
  }

  @Test
  @DisplayName("요청자 ID 헤더가 없으면 기사 뷰 등록에 실패하고 400 Bad Request로 응답한다")
  void shouldReturnBadRequest_whenRequestUserIdHeaderIsMissing() throws Exception {
    // given
    NewsArticle article = saveArticle();

    // when & then
    mockMvc.perform(post("/api/articles/{articleId}/article-views", article.getId()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.header").value(REQUEST_USER_ID_HEADER));
  }

  @Test
  @DisplayName("존재하지 않는 기사에 대한 뷰 등록을 요청하면 404 Not Found로 응답한다")
  void shouldReturnNotFound_whenArticleDoesNotExist() throws Exception {
    // given
    User user = saveUser();
    UUID missingArticleId = UUID.randomUUID();

    // when & then
    mockMvc.perform(post("/api/articles/{articleId}/article-views", missingArticleId)
            .header(REQUEST_USER_ID_HEADER, user.getId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ARTICLE_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.details.articleId").value(missingArticleId.toString()));
  }

  @Test
  @DisplayName("삭제된 기사에 대한 뷰 등록을 요청하면 400 Bad Request로 응답한다")
  void shouldReturnBadRequest_whenArticleIsDeleted() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User user = saveUser();
    markArticleDeleted(article);

    // when & then
    mockMvc.perform(post("/api/articles/{articleId}/article-views", article.getId())
            .header(REQUEST_USER_ID_HEADER, user.getId()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ARTICLE_DELETED"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.details.articleId").value(article.getId().toString()));
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
        "test article",
        Instant.parse("2026-04-28T09:00:00Z"),
        "test article summary"
    );

    entityManager.persist(source);
    entityManager.persist(article);
    entityManager.flush();
    entityManager.clear();

    return entityManager.find(NewsArticle.class, article.getId());
  }

  private User saveUser() {
    UUID id = UUID.randomUUID();
    User user = User.create(
        "user-" + id + "@example.com",
        "test-user",
        "encoded-password"
    );

    entityManager.persist(user);
    entityManager.flush();
    entityManager.clear();

    return entityManager.find(User.class, user.getId());
  }

  private void markArticleDeleted(NewsArticle article) {
    NewsArticle articleReference = entityManager.getReference(NewsArticle.class, article.getId());
    articleReference.markDeleted();
    entityManager.flush();
    entityManager.clear();
  }

  private Long countArticleViews(NewsArticle article, User user) {
    return entityManager.createQuery(
            """
                select count(av) from ArticleView av
                where av.article.id = :articleId and av.user.id = :userId
                """,
            Long.class
        )
        .setParameter("articleId", article.getId())
        .setParameter("userId", user.getId())
        .getSingleResult();
  }

  private Integer findArticleViewCount(NewsArticle article) {
    return entityManager.createQuery(
            "select a.viewCount from NewsArticle a where a.id = :articleId",
            Integer.class
        )
        .setParameter("articleId", article.getId())
        .getSingleResult();
  }

  private String findSourceName(UUID articleId) {
    return entityManager.createQuery(
            """
                select s.name
                from NewsArticle a
                join a.source s
                where a.id = :articleId
                """,
            String.class
        )
        .setParameter("articleId", articleId)
        .getSingleResult();
  }

  private Instant findLastViewedAt(UUID articleViewId) {
    return entityManager.createQuery(
            "select av.lastViewedAt from ArticleView av where av.id = :articleViewId",
            Instant.class
        )
        .setParameter("articleViewId", articleViewId)
        .getSingleResult();
  }

  private JsonNode registerArticleView(UUID articleId, UUID userId) throws Exception {
    String responseBody = mockMvc.perform(post("/api/articles/{articleId}/article-views", articleId)
            .header(REQUEST_USER_ID_HEADER, userId))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    return objectMapper.readTree(responseBody);
  }
}

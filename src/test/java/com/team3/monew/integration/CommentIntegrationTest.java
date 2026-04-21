package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.dto.comment.CommentUpdateRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.CommentLike;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.entity.enums.NotificationResourceType;
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

  private static final String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";

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
        user.getId(),
        "댓글 내용입니다."
    );

    // when & then
    mockMvc.perform(post("/api/comments")
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
    CommentRegisterRequest request = new CommentRegisterRequest(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "댓글 내용입니다."
    );

    // when & then
    mockMvc.perform(post("/api/comments")
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

  @Test
  @DisplayName("유효한 요청이면 댓글 수정에 성공하고 댓글 내용을 변경한다.")
  void shouldUpdateComment_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User user = saveUser();
    Comment comment = saveComment(article, user, "댓글 내용입니다.");
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글 내용입니다.");

    // when & then
    mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
            .header(REQUEST_USER_ID_HEADER, user.getId())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(comment.getId().toString()))
        .andExpect(jsonPath("$.articleId").value(article.getId().toString()))
        .andExpect(jsonPath("$.userId").value(user.getId().toString()))
        .andExpect(jsonPath("$.userNickname").value(user.getNickname()))
        .andExpect(jsonPath("$.content").value("수정된 댓글 내용입니다."))
        .andExpect(jsonPath("$.likeCount").value(0))
        .andExpect(jsonPath("$.likedByMe").value(false));

    entityManager.flush();
    entityManager.clear();

    String savedContent = entityManager.createQuery(
            "select c.content from Comment c where c.id = :commentId",
            String.class
        )
        .setParameter("commentId", comment.getId())
        .getSingleResult();

    assertThat(savedContent).isEqualTo("수정된 댓글 내용입니다.");
  }

  @Test
  @DisplayName("존재하지 않는 댓글을 수정하면 404 Not Found로 응답한다.")
  void shouldReturnNotFound_whenCommentDoesNotExist() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글 내용입니다.");

    // when & then
    mockMvc.perform(patch("/api/comments/{commentId}", commentId)
            .header(REQUEST_USER_ID_HEADER, UUID.randomUUID())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));
  }

  @Test
  @DisplayName("작성자가 아닌 사용자가 댓글을 수정하면 403 Forbidden으로 응답하고 댓글 내용을 변경하지 않는다.")
  void shouldReturnForbidden_whenUserIsNotAuthor() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User author = saveUser();
    User otherUser = saveUser();
    Comment comment = saveComment(article, author, "댓글 내용입니다.");
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글 내용입니다.");

    // when & then
    mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
            .header(REQUEST_USER_ID_HEADER, otherUser.getId())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("COMMENT_UPDATE_FORBIDDEN"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.details.commentId").value(comment.getId().toString()));

    entityManager.flush();
    entityManager.clear();

    String savedContent = entityManager.createQuery(
            "select c.content from Comment c where c.id = :commentId",
            String.class
        )
        .setParameter("commentId", comment.getId())
        .getSingleResult();

    assertThat(savedContent).isEqualTo("댓글 내용입니다.");
  }

  @Test
  @DisplayName("유효한 요청이면 댓글 삭제에 성공하고 댓글이 삭제 상태로 변경되며 기사 댓글 수가 감소한다.")
  void shouldDeleteComment_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User user = saveUser();
    UUID commentId = registerComment(article.getId(), user.getId(), "삭제할 댓글입니다.");

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}", commentId))
        .andExpect(status().isNoContent());

    entityManager.flush();
    entityManager.clear();

    Object[] deletedCommentState = entityManager.createQuery(
            "select c.deleteStatus, c.deletedAt from Comment c where c.id = :commentId",
            Object[].class
        )
        .setParameter("commentId", commentId)
        .getSingleResult();
    NewsArticle savedArticle = newsArticleRepository.findById(article.getId()).orElseThrow();

    assertThat(deletedCommentState[0].toString()).isEqualTo("DELETED");
    assertThat(deletedCommentState[1]).isNotNull();
    assertThat(savedArticle.getCommentCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("존재하지 않는 댓글을 삭제하면 404 Not Found로 응답한다.")
  void shouldReturnNotFound_whenDeleteCommentDoesNotExist() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}", commentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));
  }

  @Test
  @DisplayName("이미 삭제된 댓글을 삭제하면 404 Not Found로 응답한다.")
  void shouldReturnNotFound_whenDeleteCommentIsAlreadyDeleted() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User user = saveUser();
    UUID commentId = registerComment(article.getId(), user.getId(), "삭제할 댓글입니다.");

    mockMvc.perform(delete("/api/comments/{commentId}", commentId))
        .andExpect(status().isNoContent());

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}", commentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMENT_DELETED"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));
  }

  @Test
  @DisplayName("유효한 요청이면 댓글 물리 삭제에 성공하고 관련 데이터가 함께 삭제된다.")
  void shouldHardDeleteComment_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User writer = saveUser();
    User actor = saveUser();
    UUID commentId = registerComment(article.getId(), writer.getId(), "삭제할 댓글입니다.");

    Comment commentReference = entityManager.getReference(Comment.class, commentId);
    User actorReference = entityManager.getReference(User.class, actor.getId());
    User writerReference = entityManager.getReference(User.class, writer.getId());
    entityManager.persist(CommentLike.create(commentReference, actorReference));
    entityManager.persist(
        Notification.create(
            writerReference,
            "좋아요 알림",
            NotificationResourceType.COMMENT,
            commentId,
            actorReference
        )
    );
    entityManager.flush();
    entityManager.clear();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/hard", commentId))
        .andExpect(status().isNoContent());

    entityManager.flush();
    entityManager.clear();

    Long commentCount = entityManager.createQuery(
            "select count(c) from Comment c where c.id = :commentId",
            Long.class
        )
        .setParameter("commentId", commentId)
        .getSingleResult();
    Long commentLikeCount = entityManager.createQuery(
            "select count(cl) from CommentLike cl where cl.comment.id = :commentId",
            Long.class
        )
        .setParameter("commentId", commentId)
        .getSingleResult();
    Long notificationCount = entityManager.createQuery(
            """
            select count(n) from Notification n
            where n.resourceType = :resourceType and n.resourceId = :commentId
            """,
            Long.class
        )
        .setParameter("resourceType", NotificationResourceType.COMMENT)
        .setParameter("commentId", commentId)
        .getSingleResult();
    NewsArticle savedArticle = newsArticleRepository.findById(article.getId()).orElseThrow();

    assertThat(commentCount).isEqualTo(0);
    assertThat(commentLikeCount).isEqualTo(0);
    assertThat(notificationCount).isEqualTo(0);
    assertThat(savedArticle.getCommentCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("존재하지 않는 댓글을 물리 삭제하면 404 Not Found로 응답한다.")
  void shouldReturnNotFound_whenHardDeleteCommentDoesNotExist() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/hard", commentId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));
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
        "테스트",
        "encoded-password"
    );

    entityManager.persist(user);
    entityManager.flush();
    entityManager.clear();
    return user;
  }

  private Comment saveComment(NewsArticle article, User user, String content) {
    NewsArticle articleReference = entityManager.getReference(NewsArticle.class, article.getId());
    User userReference = entityManager.getReference(User.class, user.getId());
    Comment comment = Comment.create(articleReference, userReference, content);

    entityManager.persist(comment);
    entityManager.flush();
    entityManager.clear();
    return comment;
  }

  private UUID registerComment(UUID articleId, UUID userId, String content) throws Exception {
    CommentRegisterRequest request = new CommentRegisterRequest(articleId, userId, content);

    String responseBody = mockMvc.perform(post("/api/comments")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode jsonNode = objectMapper.readTree(responseBody);
    return UUID.fromString(jsonNode.get("id").asText());
  }
}

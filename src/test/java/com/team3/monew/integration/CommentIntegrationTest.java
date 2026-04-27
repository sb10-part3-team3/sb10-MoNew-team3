package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.team3.monew.entity.enums.DeleteStatus;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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
class CommentIntegrationTest extends IntegrationTestSupport {

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
  @DisplayName("댓글 등록 요청이 성공하면 댓글이 생성되고 기사 댓글 수가 증가한다")
  void shouldRegisterComment_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User user = saveUser();
    CommentRegisterRequest request = new CommentRegisterRequest(
        article.getId(),
        user.getId(),
        "comment content"
    );

    // when & then
    mockMvc.perform(post("/api/comments")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.articleId").value(article.getId().toString()))
        .andExpect(jsonPath("$.userId").value(user.getId().toString()))
        .andExpect(jsonPath("$.userNickname").value(user.getNickname()))
        .andExpect(jsonPath("$.content").value("comment content"))
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
  @DisplayName("댓글 수정 요청이 성공하면 댓글 내용이 변경된다")
  void shouldUpdateComment_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User user = saveUser();
    Comment comment = saveComment(article, user, "comment content");
    CommentUpdateRequest request = new CommentUpdateRequest("updated comment content");

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
        .andExpect(jsonPath("$.content").value("updated comment content"))
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

    assertThat(savedContent).isEqualTo("updated comment content");
  }

  @Test
  @DisplayName("댓글 작성자가 아닌 사용자는 댓글을 수정할 수 없다")
  void shouldReturnForbidden_whenUserIsNotAuthor() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User author = saveUser();
    User otherUser = saveUser();
    Comment comment = saveComment(article, author, "comment content");
    CommentUpdateRequest request = new CommentUpdateRequest("updated comment content");

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

    assertThat(savedContent).isEqualTo("comment content");
  }

  @Test
  @DisplayName("댓글 삭제 요청이 성공하면 댓글이 논리 삭제되고 기사 댓글 수가 감소한다")
  void shouldDeleteComment_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User user = saveUser();
    UUID commentId = registerComment(article.getId(), user.getId(), "comment to delete");

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

    assertThat(deletedCommentState[0]).isEqualTo(DeleteStatus.DELETED);
    assertThat(deletedCommentState[1]).isNotNull();
    assertThat(savedArticle.getCommentCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("댓글 물리 삭제 요청이 성공하면 댓글과 연관 데이터가 함께 삭제된다")
  void shouldHardDeleteComment_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User writer = saveUser();
    User actor = saveUser();
    UUID commentId = registerComment(article.getId(), writer.getId(), "comment to hard delete");

    Comment commentReference = entityManager.getReference(Comment.class, commentId);
    User actorReference = entityManager.getReference(User.class, actor.getId());
    User writerReference = entityManager.getReference(User.class, writer.getId());
    entityManager.persist(CommentLike.create(commentReference, actorReference));
    entityManager.persist(
        Notification.create(
            writerReference,
            "like notification",
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
  @DisplayName("댓글 목록 조회 요청이 성공하면 대상 기사의 활성 댓글만 반환한다")
  void shouldFindComments_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    NewsArticle otherArticle = saveArticle();
    User writer = saveUser();
    User requestUser = saveUser();
    Comment first = saveComment(article, writer, "first comment",
        Instant.parse("2026-04-17T00:00:03Z"), 2);
    Comment second = saveComment(article, writer, "second comment",
        Instant.parse("2026-04-17T00:00:02Z"), 1);
    Comment deleted = saveComment(article, writer, "deleted comment",
        Instant.parse("2026-04-17T00:00:04Z"), 10);

    saveCommentLike(second, requestUser);
    saveComment(otherArticle, writer, "other article comment",
        Instant.parse("2026-04-17T00:00:05Z"), 5);
    markCommentDeleted(deleted);

    // when & then
    mockMvc.perform(get("/api/comments")
            .header(REQUEST_USER_ID_HEADER, requestUser.getId())
            .param("articleId", article.getId().toString())
            .param("orderBy", "createdAt")
            .param("direction", "DESC")
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(first.getId().toString()))
        .andExpect(jsonPath("$.content[0].content").value("first comment"))
        .andExpect(jsonPath("$.content[0].likedByMe").value(false))
        .andExpect(jsonPath("$.content[1].id").value(second.getId().toString()))
        .andExpect(jsonPath("$.content[1].content").value("second comment"))
        .andExpect(jsonPath("$.content[1].likedByMe").value(true))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("존재하지 않는 기사로 댓글 목록을 조회하면 400을 반환한다")
  void shouldReturnBadRequest_whenArticleDoesNotExist() throws Exception {
    // given
    User requestUser = saveUser();

    // when & then
    mockMvc.perform(get("/api/comments")
            .header(REQUEST_USER_ID_HEADER, requestUser.getId())
            .param("articleId", UUID.randomUUID().toString())
            .param("orderBy", "createdAt")
            .param("direction", "DESC")
            .param("limit", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
        .andExpect(jsonPath("$.details.articleId").exists());
  }

  @Test
  @DisplayName("등록순 커서가 있으면 다음 페이지 댓글을 조회한다")
  void shouldFindNextPageCommentsByCreatedAt_whenCursorExists() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User writer = saveUser();
    User requestUser = saveUser();

    Comment first = saveComment(article, writer, "first comment",
        Instant.parse("2026-04-17T00:00:03Z"), 10);
    Comment second = saveComment(article, writer, "second comment",
        Instant.parse("2026-04-17T00:00:02Z"), 7);
    Comment third = saveComment(article, writer, "third comment",
        Instant.parse("2026-04-17T00:00:01Z"), 5);

    JsonNode firstPage = objectMapper.readTree(mockMvc.perform(get("/api/comments")
            .header(REQUEST_USER_ID_HEADER, requestUser.getId())
            .param("articleId", article.getId().toString())
            .param("orderBy", "createdAt")
            .param("direction", "DESC")
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(first.getId().toString()))
        .andExpect(jsonPath("$.content[1].id").value(second.getId().toString()))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andReturn()
        .getResponse()
        .getContentAsString());
    String nextCursor = firstPage.get("nextCursor").asText();

    mockMvc.perform(get("/api/comments")
            .header(REQUEST_USER_ID_HEADER, requestUser.getId())
            .param("articleId", article.getId().toString())
            .param("orderBy", "createdAt")
            .param("direction", "DESC")
            .param("cursor", nextCursor)
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(third.getId().toString()))
        .andExpect(jsonPath("$.content[0].content").value("third comment"))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("좋아요순 커서가 있으면 다음 페이지 댓글을 조회한다")
  void shouldFindNextPageCommentsByLikeCount_whenCursorExists() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User writer = saveUser();
    User requestUser = saveUser();

    Comment first = saveComment(article, writer, "first comment",
        Instant.parse("2026-04-17T00:00:03Z"), 10);
    Comment second = saveComment(article, writer, "second comment",
        Instant.parse("2026-04-17T00:00:02Z"), 7);
    Comment third = saveComment(article, writer, "third comment",
        Instant.parse("2026-04-17T00:00:01Z"), 7);

    JsonNode firstPage = objectMapper.readTree(mockMvc.perform(get("/api/comments")
            .header(REQUEST_USER_ID_HEADER, requestUser.getId())
            .param("articleId", article.getId().toString())
            .param("orderBy", "likeCount")
            .param("direction", "DESC")
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(first.getId().toString()))
        .andExpect(jsonPath("$.content[1].id").value(second.getId().toString()))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andReturn()
        .getResponse()
        .getContentAsString());
    String nextCursor = firstPage.get("nextCursor").asText();

    mockMvc.perform(get("/api/comments")
            .header(REQUEST_USER_ID_HEADER, requestUser.getId())
            .param("articleId", article.getId().toString())
            .param("orderBy", "likeCount")
            .param("direction", "DESC")
            .param("cursor", nextCursor)
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(third.getId().toString()))
        .andExpect(jsonPath("$.content[0].content").value("third comment"))
        .andExpect(jsonPath("$.content[0].likeCount").value(7))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("등록 시각이 같은 댓글도 커서 페이지를 넘겨 모두 조회할 수 있다")
  void shouldFindAllCommentsAcrossPages_whenCreatedAtIsSame() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User writer = saveUser();
    User requestUser = saveUser();
    Instant sameCreatedAt = Instant.parse("2026-04-17T00:00:03Z");

    Comment first = saveComment(article, writer, "first comment", sameCreatedAt, 3);
    Comment second = saveComment(article, writer, "second comment", sameCreatedAt, 2);
    Comment third = saveComment(article, writer, "third comment", sameCreatedAt, 1);

    JsonNode firstPage = objectMapper.readTree(mockMvc.perform(get("/api/comments")
            .header(REQUEST_USER_ID_HEADER, requestUser.getId())
            .param("articleId", article.getId().toString())
            .param("orderBy", "createdAt")
            .param("direction", "DESC")
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andReturn()
        .getResponse()
        .getContentAsString());
    String nextCursor = firstPage.get("nextCursor").asText();

    JsonNode secondPage = objectMapper.readTree(mockMvc.perform(get("/api/comments")
            .header(REQUEST_USER_ID_HEADER, requestUser.getId())
            .param("articleId", article.getId().toString())
            .param("orderBy", "createdAt")
            .param("direction", "DESC")
            .param("cursor", nextCursor)
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andReturn()
        .getResponse()
        .getContentAsString());

    Set<String> actualCommentIds = collectCommentIds(firstPage, secondPage);

    assertThat(actualCommentIds).containsExactlyInAnyOrder(
        first.getId().toString(),
        second.getId().toString(),
        third.getId().toString()
    );
  }

  @Test
  @DisplayName("좋아요 수와 등록 시각이 같은 댓글도 커서 페이지를 넘겨 모두 조회할 수 있다")
  void shouldFindAllCommentsAcrossPages_whenLikeCountAndCreatedAtAreSame() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User writer = saveUser();
    User requestUser = saveUser();
    Instant sameCreatedAt = Instant.parse("2026-04-17T00:00:03Z");

    Comment first = saveComment(article, writer, "first comment", sameCreatedAt, 7);
    Comment second = saveComment(article, writer, "second comment", sameCreatedAt, 7);
    Comment third = saveComment(article, writer, "third comment", sameCreatedAt, 7);

    JsonNode firstPage = objectMapper.readTree(mockMvc.perform(get("/api/comments")
            .header(REQUEST_USER_ID_HEADER, requestUser.getId())
            .param("articleId", article.getId().toString())
            .param("orderBy", "likeCount")
            .param("direction", "DESC")
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andReturn()
        .getResponse()
        .getContentAsString());
    String nextCursor = firstPage.get("nextCursor").asText();

    JsonNode secondPage = objectMapper.readTree(mockMvc.perform(get("/api/comments")
            .header(REQUEST_USER_ID_HEADER, requestUser.getId())
            .param("articleId", article.getId().toString())
            .param("orderBy", "likeCount")
            .param("direction", "DESC")
            .param("cursor", nextCursor)
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andReturn()
        .getResponse()
        .getContentAsString());

    Set<String> actualCommentIds = collectCommentIds(firstPage, secondPage);

    assertThat(actualCommentIds).containsExactlyInAnyOrder(
        first.getId().toString(),
        second.getId().toString(),
        third.getId().toString()
    );
  }

  @Test
  @DisplayName("댓글 좋아요 요청이 성공하면 좋아요가 생성되고 좋아요 수가 증가한다")
  void shouldLikeComment_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User writer = saveUser();
    User liker = saveUser();
    Comment comment = saveComment(article, writer, "comment to like");

    // when & then
    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", comment.getId())
            .header(REQUEST_USER_ID_HEADER, liker.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.likedBy").value(liker.getId().toString()))
        .andExpect(jsonPath("$.commentId").value(comment.getId().toString()))
        .andExpect(jsonPath("$.articleId").value(article.getId().toString()))
        .andExpect(jsonPath("$.commentUserId").value(writer.getId().toString()))
        .andExpect(jsonPath("$.commentUserNickname").value(writer.getNickname()))
        .andExpect(jsonPath("$.commentContent").value("comment to like"))
        .andExpect(jsonPath("$.commentLikeCount").value(1))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.commentCreatedAt").exists());

    entityManager.flush();
    entityManager.clear();

    assertThat(countCommentLikes(comment, liker)).isEqualTo(1);
    assertThat(findCommentLikeCount(comment)).isEqualTo(1);
  }

  @Test
  @DisplayName("댓글 좋아요 취소 요청이 성공하면 좋아요가 삭제되고 좋아요 수가 감소한다")
  void shouldUnlikeComment_whenRequestIsValid() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User writer = saveUser();
    User liker = saveUser();
    Comment comment = saveComment(article, writer, "comment to unlike",
        Instant.parse("2026-04-17T00:00:01Z"), 1);
    saveCommentLike(comment, liker);

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", comment.getId())
            .header(REQUEST_USER_ID_HEADER, liker.getId()))
        .andExpect(status().isOk());

    entityManager.flush();
    entityManager.clear();

    assertThat(countCommentLikes(comment, liker)).isZero();
    assertThat(findCommentLikeCount(comment)).isZero();
  }

  @Test
  @DisplayName("이미 좋아요한 댓글에 다시 좋아요를 요청하면 400을 반환하고 상태를 유지한다")
  void shouldReturnBadRequest_whenCommentLikeAlreadyExists() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User writer = saveUser();
    User liker = saveUser();
    Comment comment = saveComment(article, writer, "comment already liked",
        Instant.parse("2026-04-17T00:00:01Z"), 1);
    saveCommentLike(comment, liker);

    // when & then
    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", comment.getId())
            .header(REQUEST_USER_ID_HEADER, liker.getId()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
        .andExpect(jsonPath("$.status").value(400));

    entityManager.flush();
    entityManager.clear();

    assertThat(countCommentLikes(comment, liker)).isEqualTo(1);
    assertThat(findCommentLikeCount(comment)).isEqualTo(1);
  }

  @Test
  @DisplayName("좋아요하지 않은 댓글에 좋아요 취소를 요청하면 400을 반환하고 상태를 유지한다")
  void shouldReturnBadRequest_whenCommentLikeDoesNotExist() throws Exception {
    // given
    NewsArticle article = saveArticle();
    User writer = saveUser();
    User liker = saveUser();
    Comment comment = saveComment(article, writer, "comment without like");

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", comment.getId())
            .header(REQUEST_USER_ID_HEADER, liker.getId()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
        .andExpect(jsonPath("$.status").value(400));

    entityManager.flush();
    entityManager.clear();

    assertThat(countCommentLikes(comment, liker)).isZero();
    assertThat(findCommentLikeCount(comment)).isZero();
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
        Instant.parse("2026-04-17T00:00:00Z"),
        "test article summary"
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
        "test-user",
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
    return entityManager.find(Comment.class, comment.getId());
  }

  private Comment saveComment(
      NewsArticle article,
      User user,
      String content,
      Instant createdAt,
      int likeCount
  ) {
    Comment comment = saveComment(article, user, content);
    entityManager.createQuery(
            """
                update Comment c
                set c.createdAt = :createdAt,
                    c.updatedAt = :createdAt,
                    c.likeCount = :likeCount
                where c.id = :commentId
                """
        )
        .setParameter("createdAt", createdAt)
        .setParameter("likeCount", likeCount)
        .setParameter("commentId", comment.getId())
        .executeUpdate();
    entityManager.flush();
    entityManager.clear();
    return comment;
  }

  private void saveCommentLike(Comment comment, User user) {
    Comment commentReference = entityManager.getReference(Comment.class, comment.getId());
    User userReference = entityManager.getReference(User.class, user.getId());
    entityManager.persist(CommentLike.create(commentReference, userReference));
    entityManager.flush();
    entityManager.clear();
  }

  private void markCommentDeleted(Comment comment) {
    Comment commentReference = entityManager.getReference(Comment.class, comment.getId());
    commentReference.markDeleted();
    entityManager.flush();
    entityManager.clear();
  }

  private Long countCommentLikes(Comment comment, User user) {
    return entityManager.createQuery(
            """
                select count(cl) from CommentLike cl
                where cl.comment.id = :commentId and cl.user.id = :userId
                """,
            Long.class
        )
        .setParameter("commentId", comment.getId())
        .setParameter("userId", user.getId())
        .getSingleResult();
  }

  private Integer findCommentLikeCount(Comment comment) {
    return entityManager.createQuery(
            "select c.likeCount from Comment c where c.id = :commentId",
            Integer.class
        )
        .setParameter("commentId", comment.getId())
        .getSingleResult();
  }

  private Set<String> collectCommentIds(JsonNode... pages) {
    Set<String> commentIds = new HashSet<>();

    for (JsonNode page : pages) {
      for (JsonNode comment : page.get("content")) {
        commentIds.add(comment.get("id").asText());
      }
    }

    return commentIds;
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

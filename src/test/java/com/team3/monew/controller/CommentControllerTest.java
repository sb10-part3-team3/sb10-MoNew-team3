package com.team3.monew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentLikeDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.dto.comment.CommentUpdateRequest;
import com.team3.monew.dto.comment.CursorPageResponseCommentDto;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.comment.DeletedCommentException;
import com.team3.monew.exception.comment.UnauthorizedCommentUpdateException;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.global.exception.GlobalExceptionHandler;
import com.team3.monew.service.CommentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@WebMvcTest(CommentController.class)
@Import(GlobalExceptionHandler.class)
class CommentControllerTest {

  private static final String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private CommentService commentService;

  @Nested
  @DisplayName("댓글 등록 API를 검증한다.")
  class RegisterComment {

    @Test
    @DisplayName("유효한 요청을 받으면 댓글을 등록하고 생성된 댓글을 반환한다.")
    void shouldRegisterComment_whenRequestIsValid() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      String content = "댓글 내용입니다.";
      CommentRegisterRequest request = new CommentRegisterRequest(articleId, userId, content);
      CommentDto response = new CommentDto(
          commentId,
          articleId,
          userId,
          "테스터",
          content,
          0L,
          false,
          Instant.parse("2026-04-17T00:00:00Z")
      );

      given(commentService.registerComment(any(CommentRegisterRequest.class))).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/comments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(commentId.toString()))
          .andExpect(jsonPath("$.articleId").value(articleId.toString()))
          .andExpect(jsonPath("$.userId").value(userId.toString()))
          .andExpect(jsonPath("$.userNickname").value("테스터"))
          .andExpect(jsonPath("$.content").value(content))
          .andExpect(jsonPath("$.likeCount").value(0))
          .andExpect(jsonPath("$.likedByMe").value(false))
          .andExpect(jsonPath("$.createdAt").value("2026-04-17T00:00:00Z"));

      then(commentService).should().registerComment(any(CommentRegisterRequest.class));
      then(commentService).shouldHaveNoMoreInteractions();
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
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.details.content").exists());

      then(commentService).shouldHaveNoInteractions();
    }

  }

  @Nested
  @DisplayName("댓글 수정 API를 검증한다.")
  class UpdateComment {

    @Test
    @DisplayName("유효한 요청을 받으면 댓글을 수정하고 수정된 댓글을 반환한다.")
    void shouldUpdateComment_whenRequestIsValid() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      String content = "수정된 댓글 내용입니다.";
      CommentUpdateRequest request = new CommentUpdateRequest(content);
      CommentDto response = new CommentDto(
          commentId,
          articleId,
          userId,
          "테스터",
          content,
          0L,
          false,
          Instant.parse("2026-04-17T00:00:00Z")
      );

      given(commentService.updateComment(
          eq(commentId),
          eq(userId),
          any(CommentUpdateRequest.class)
      )).willReturn(response);

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .header(REQUEST_USER_ID_HEADER, userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(commentId.toString()))
          .andExpect(jsonPath("$.articleId").value(articleId.toString()))
          .andExpect(jsonPath("$.userId").value(userId.toString()))
          .andExpect(jsonPath("$.userNickname").value("테스터"))
          .andExpect(jsonPath("$.content").value(content))
          .andExpect(jsonPath("$.likeCount").value(0))
          .andExpect(jsonPath("$.likedByMe").value(false))
          .andExpect(jsonPath("$.createdAt").value("2026-04-17T00:00:00Z"));

      then(commentService).should().updateComment(
          eq(commentId),
          eq(userId),
          argThat(actual ->
              actual.content().equals(content)
          )
      );
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("요청자 ID 헤더가 없으면 댓글 수정에 실패하고 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenRequestUserIdHeaderIsMissing() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest(
          "수정된 댓글 내용입니다."
      );

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.details.header").value(REQUEST_USER_ID_HEADER));

      then(commentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("content가 비어 있으면 댓글 수정에 실패하고 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenContentIsEmpty() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("");

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .header(REQUEST_USER_ID_HEADER, userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.details.content").exists());

      then(commentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 수정하면 404 Not Found로 응답한다.")
    void shouldReturnNotFound_whenCommentDoesNotExist() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글 내용입니다.");
      given(commentService.updateComment(
          eq(commentId),
          eq(userId),
          any(CommentUpdateRequest.class)
      )).willThrow(new CommentNotFoundException(commentId));

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .header(REQUEST_USER_ID_HEADER, userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().updateComment(
          eq(commentId),
          eq(userId),
          any(CommentUpdateRequest.class)
      );
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("삭제된 댓글을 수정하면 404 Not Found로 응답한다.")
    void shouldReturnNotFound_whenCommentIsDeleted() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글 내용입니다.");
      given(commentService.updateComment(
          eq(commentId),
          eq(userId),
          any(CommentUpdateRequest.class)
      )).willThrow(new DeletedCommentException(commentId));

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .header(REQUEST_USER_ID_HEADER, userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_DELETED"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().updateComment(
          eq(commentId),
          eq(userId),
          any(CommentUpdateRequest.class)
      );
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 댓글을 수정하면 403 Forbidden으로 응답한다.")
    void shouldReturnForbidden_whenUserIsNotAuthor() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글 내용입니다.");
      given(commentService.updateComment(
          eq(commentId),
          eq(userId),
          any(CommentUpdateRequest.class)
      )).willThrow(new UnauthorizedCommentUpdateException(commentId));

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .header(REQUEST_USER_ID_HEADER, userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("COMMENT_UPDATE_FORBIDDEN"))
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().updateComment(
          eq(commentId),
          eq(userId),
          any(CommentUpdateRequest.class)
      );
      then(commentService).shouldHaveNoMoreInteractions();
    }

  }

  @Nested
  @DisplayName("댓글 삭제 API를 검증한다.")
  class DeleteComment {

    @Test
    @DisplayName("유효한 요청을 받으면 댓글을 삭제하고 204 No Content로 응답한다.")
    void shouldDeleteComment_whenRequestIsValid() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId))
          .andExpect(status().isNoContent());

      then(commentService).should().deleteComment(commentId);
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 삭제하면 404 Not Found로 응답한다.")
    void shouldReturnNotFound_whenDeleteCommentDoesNotExist() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      willThrow(new CommentNotFoundException(commentId))
          .given(commentService)
          .deleteComment(commentId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().deleteComment(commentId);
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("삭제된 댓글을 삭제하면 404 Not Found로 응답한다.")
    void shouldReturnNotFound_whenDeleteCommentIsAlreadyDeleted() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      willThrow(new DeletedCommentException(commentId))
          .given(commentService)
          .deleteComment(commentId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_DELETED"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().deleteComment(commentId);
      then(commentService).shouldHaveNoMoreInteractions();
    }
  }

  @Nested
  @DisplayName("댓글 물리 삭제 API를 검증한다.")
  class HardDeleteComment {

    @Test
    @DisplayName("유효한 요청을 받으면 댓글을 물리 삭제하고 204 No Content로 응답한다.")
    void shouldHardDeleteComment_whenRequestIsValid() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/hard", commentId))
          .andExpect(status().isNoContent());

      then(commentService).should().hardDeleteComment(commentId);
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 물리 삭제하면 404 Not Found로 응답한다.")
    void shouldReturnNotFound_whenHardDeleteCommentDoesNotExist() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      willThrow(new CommentNotFoundException(commentId))
          .given(commentService)
          .hardDeleteComment(commentId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/hard", commentId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().hardDeleteComment(commentId);
      then(commentService).shouldHaveNoMoreInteractions();
    }
  }

  @Nested
  @DisplayName("댓글 조회 API를 검증한다.")
  class FindComments {

    @Test
    @DisplayName("유효한 요청을 받으면 댓글 목록 페이지를 반환한다.")
    void shouldFindComments_whenRequestIsValid() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      UUID firstCommentId = UUID.randomUUID();
      UUID secondCommentId = UUID.randomUUID();
      int limit = 2;
      CommentDto firstComment = new CommentDto(
          firstCommentId,
          articleId,
          UUID.randomUUID(),
          "첫 번째 작성자",
          "첫 번째 댓글입니다.",
          3L,
          false,
          Instant.parse("2026-04-17T00:00:03Z")
      );
      CommentDto secondComment = new CommentDto(
          secondCommentId,
          articleId,
          userId,
          "두 번째 작성자",
          "두 번째 댓글입니다.",
          1L,
          true,
          Instant.parse("2026-04-17T00:00:02Z")
      );
      CursorPageResponseCommentDto response = new CursorPageResponseCommentDto(
          List.of(firstComment, secondComment),
          "2026-04-17T00:00:02Z",
          Instant.parse("2026-04-17T00:00:02Z"),
          2,
          3L,
          true
      );

      given(commentService.findComments(
          articleId,
          "createdAt",
          "DESC",
          null,
          null,
          limit,
          userId
      )).willReturn(response);

      // when & then
      mockMvc.perform(get("/api/comments")
              .header(REQUEST_USER_ID_HEADER, userId)
              .param("articleId", articleId.toString())
              .param("orderBy", "createdAt")
              .param("direction", "DESC")
              .param("limit", String.valueOf(limit)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(firstCommentId.toString()))
          .andExpect(jsonPath("$.content[0].likedByMe").value(false))
          .andExpect(jsonPath("$.content[1].id").value(secondCommentId.toString()))
          .andExpect(jsonPath("$.content[1].likedByMe").value(true))
          .andExpect(jsonPath("$.nextCursor").value("2026-04-17T00:00:02Z"))
          .andExpect(jsonPath("$.nextAfter").value("2026-04-17T00:00:02Z"))
          .andExpect(jsonPath("$.size").value(2))
          .andExpect(jsonPath("$.totalElements").value(3))
          .andExpect(jsonPath("$.hasNext").value(true));

      then(commentService).should().findComments(
          articleId,
          "createdAt",
          "DESC",
          null,
          null,
          limit,
          userId
      );
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("요청자 ID 헤더가 없으면 댓글 조회에 실패하고 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenRequestUserIdHeaderIsMissing() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();

      // when & then
      mockMvc.perform(get("/api/comments")
              .param("articleId", articleId.toString())
              .param("orderBy", "createdAt")
              .param("direction", "DESC")
              .param("limit", "10"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.details.header").value(REQUEST_USER_ID_HEADER));

      then(commentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 기사면 댓글 조회에 실패하고 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenArticleDoesNotExist() throws Exception {
      // given
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      given(commentService.findComments(
          articleId,
          "createdAt",
          "DESC",
          null,
          null,
          10,
          userId
      )).willThrow(new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE,
          Map.of("articleId", articleId.toString())
      ));

      // when & then
      mockMvc.perform(get("/api/comments")
              .header(REQUEST_USER_ID_HEADER, userId)
              .param("articleId", articleId.toString())
              .param("orderBy", "createdAt")
              .param("direction", "DESC")
              .param("limit", "10"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.details.articleId").value(articleId.toString()));

      then(commentService).should().findComments(
          articleId,
          "createdAt",
          "DESC",
          null,
          null,
          10,
          userId
      );
      then(commentService).shouldHaveNoMoreInteractions();
    }

  }

  @Nested
  @DisplayName("댓글 좋아요 등록 API를 검증한다.")
  class LikeComment {

    @Test
    @DisplayName("요청자 ID 헤더와 댓글 ID를 받으면 댓글 좋아요를 등록하고 좋아요 정보를 반환한다.")
    void shouldLikeComment_whenRequestIsValid() throws Exception {
      // given
      UUID commentLikeId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      UUID commentUserId = UUID.randomUUID();
      Instant likedAt = Instant.parse("2026-04-17T00:00:02Z");
      Instant commentCreatedAt = Instant.parse("2026-04-17T00:00:00Z");
      CommentLikeDto response = new CommentLikeDto(
          commentLikeId,
          requestUserId,
          likedAt,
          commentId,
          articleId,
          commentUserId,
          "댓글작성자",
          "댓글 내용입니다.",
          1L,
          commentCreatedAt
      );

      given(commentService.likeComment(commentId, requestUserId)).willReturn(response);

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
              .header(REQUEST_USER_ID_HEADER, requestUserId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(commentLikeId.toString()))
          .andExpect(jsonPath("$.likedBy").value(requestUserId.toString()))
          .andExpect(jsonPath("$.createdAt").value("2026-04-17T00:00:02Z"))
          .andExpect(jsonPath("$.commentId").value(commentId.toString()))
          .andExpect(jsonPath("$.articleId").value(articleId.toString()))
          .andExpect(jsonPath("$.commentUserId").value(commentUserId.toString()))
          .andExpect(jsonPath("$.commentUserNickname").value("댓글작성자"))
          .andExpect(jsonPath("$.commentContent").value("댓글 내용입니다."))
          .andExpect(jsonPath("$.commentLikeCount").value(1))
          .andExpect(jsonPath("$.commentCreatedAt").value("2026-04-17T00:00:00Z"));

      then(commentService).should().likeComment(commentId, requestUserId);
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("요청자 ID 헤더가 없으면 댓글 좋아요 등록에 실패하고 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenRequestUserIdHeaderIsMissing() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.details.header").value(REQUEST_USER_ID_HEADER));

      then(commentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 댓글에 좋아요를 등록하면 404 Not Found로 응답한다.")
    void shouldReturnNotFound_whenLikeCommentDoesNotExist() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      given(commentService.likeComment(commentId, requestUserId))
          .willThrow(new CommentNotFoundException(commentId));

      // when & then
      mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
              .header(REQUEST_USER_ID_HEADER, requestUserId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().likeComment(commentId, requestUserId);
      then(commentService).shouldHaveNoMoreInteractions();
    }
  }

  @Nested
  @DisplayName("댓글 좋아요 취소 API를 검증한다.")
  class UnlikeComment {

    @Test
    @DisplayName("요청자 ID 헤더와 댓글 ID를 받으면 댓글 좋아요를 취소하고 200 OK로 응답한다.")
    void shouldUnlikeComment_whenRequestIsValid() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
              .header(REQUEST_USER_ID_HEADER, requestUserId))
          .andExpect(status().isOk());

      then(commentService).should().unlikeComment(commentId, requestUserId);
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("요청자 ID 헤더가 없으면 댓글 좋아요 취소에 실패하고 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenRequestUserIdHeaderIsMissing() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.details.header").value(REQUEST_USER_ID_HEADER));

      then(commentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 댓글의 좋아요를 취소하면 404 Not Found로 응답한다.")
    void shouldReturnNotFound_whenUnlikeCommentDoesNotExist() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();
      willThrow(new CommentNotFoundException(commentId))
          .given(commentService)
          .unlikeComment(commentId, requestUserId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
              .header(REQUEST_USER_ID_HEADER, requestUserId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().unlikeComment(commentId, requestUserId);
      then(commentService).shouldHaveNoMoreInteractions();
    }
  }
}

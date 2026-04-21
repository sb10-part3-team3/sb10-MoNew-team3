package com.team3.monew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.dto.comment.CommentUpdateRequest;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.comment.DeletedCommentException;
import com.team3.monew.exception.comment.UnauthorizedCommentDeleteException;
import com.team3.monew.exception.comment.UnauthorizedCommentException;
import com.team3.monew.global.exception.GlobalExceptionHandler;
import com.team3.monew.service.CommentService;
import java.time.Instant;
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

    @Test
    @DisplayName("content가 500자를 초과하면 댓글 등록에 실패하고 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenContentExceedsMaxLength() throws Exception {
      // given
      CommentRegisterRequest request = new CommentRegisterRequest(
          UUID.randomUUID(),
          UUID.randomUUID(),
          "a".repeat(501)
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

    @Test
    @DisplayName("댓글 등록 중 예상하지 못한 오류가 발생하면 500 Internal Server Error로 응답한다.")
    void shouldReturnInternalServerError_whenUnexpectedExceptionOccurs() throws Exception {
      // given
      CommentRegisterRequest request = new CommentRegisterRequest(
          UUID.randomUUID(),
          UUID.randomUUID(),
          "댓글 내용입니다."
      );
      given(commentService.registerComment(any(CommentRegisterRequest.class)))
          .willThrow(new RuntimeException("unexpected error"));

      // when & then
      mockMvc.perform(post("/api/comments")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
          .andExpect(jsonPath("$.status").value(500));

      then(commentService).should().registerComment(any(CommentRegisterRequest.class));
      then(commentService).shouldHaveNoMoreInteractions();
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
    @DisplayName("content가 500자를 초과하면 댓글 수정에 실패하고 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenContentExceedsMaxLength() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("a".repeat(501));

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
      )).willThrow(new UnauthorizedCommentException(commentId));

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

    @Test
    @DisplayName("댓글 수정 중 예상하지 못한 오류가 발생하면 500 Internal Server Error로 응답한다.")
    void shouldReturnInternalServerError_whenUnexpectedExceptionOccurs() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      CommentUpdateRequest request = new CommentUpdateRequest("수정된 댓글 내용입니다.");
      given(commentService.updateComment(
          eq(commentId),
          eq(userId),
          any(CommentUpdateRequest.class)
      )).willThrow(new RuntimeException("unexpected error"));

      // when & then
      mockMvc.perform(patch("/api/comments/{commentId}", commentId)
              .header(REQUEST_USER_ID_HEADER, userId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
          .andExpect(jsonPath("$.status").value(500));

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
      UUID userId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId)
              .header(REQUEST_USER_ID_HEADER, userId))
          .andExpect(status().isNoContent());

      then(commentService).should().deleteComment(commentId, userId);
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("요청자 ID 헤더가 없으면 댓글 삭제에 실패하고 400 Bad Request로 응답한다.")
    void shouldReturnBadRequest_whenRequestUserIdHeaderIsMissing() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.details.header").value(REQUEST_USER_ID_HEADER));

      then(commentService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 댓글을 삭제하면 404 Not Found로 응답한다.")
    void shouldReturnNotFound_whenDeleteCommentDoesNotExist() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      willThrow(new CommentNotFoundException(commentId))
          .given(commentService)
          .deleteComment(commentId, userId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId)
              .header(REQUEST_USER_ID_HEADER, userId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().deleteComment(commentId, userId);
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("이미 삭제된 댓글을 삭제하면 404 Not Found로 응답한다.")
    void shouldReturnNotFound_whenDeleteCommentIsAlreadyDeleted() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      willThrow(new DeletedCommentException(commentId))
          .given(commentService)
          .deleteComment(commentId, userId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId)
              .header(REQUEST_USER_ID_HEADER, userId))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("COMMENT_DELETED"))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().deleteComment(commentId, userId);
      then(commentService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("작성자가 아닌 사용자가 댓글을 삭제하면 403 Forbidden으로 응답한다.")
    void shouldReturnForbidden_whenUserIsNotAuthor() throws Exception {
      // given
      UUID commentId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      willThrow(new UnauthorizedCommentDeleteException(commentId))
          .given(commentService)
          .deleteComment(commentId, userId);

      // when & then
      mockMvc.perform(delete("/api/comments/{commentId}", commentId)
              .header(REQUEST_USER_ID_HEADER, userId))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("COMMENT_DELETE_FORBIDDEN"))
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(jsonPath("$.details.commentId").value(commentId.toString()));

      then(commentService).should().deleteComment(commentId, userId);
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
}

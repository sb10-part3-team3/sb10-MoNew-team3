package com.team3.monew.controller;

import com.team3.monew.controller.api.CommentApi;
import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentLikeDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.dto.comment.CommentUpdateRequest;
import com.team3.monew.dto.comment.CursorPageResponseCommentDto;
import com.team3.monew.service.CommentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController implements CommentApi {

  private final CommentService commentService;

  @PostMapping
  public ResponseEntity<CommentDto> registerComment(
      @Valid @RequestBody CommentRegisterRequest commentRegisterRequest
  ) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(commentService.registerComment(commentRegisterRequest));
  }

  @PatchMapping("/{commentId}")
  public ResponseEntity<CommentDto> updateComment(
      @PathVariable("commentId") UUID commentId,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId,
      @Valid @RequestBody CommentUpdateRequest commentUpdateRequest
  ) {
    return ResponseEntity.ok(
        commentService.updateComment(commentId, requestUserId, commentUpdateRequest)
    );
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable("commentId") UUID commentId,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId
  ) {
    commentService.deleteComment(commentId, requestUserId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{commentId}/hard")
  public ResponseEntity<Void> hardDeleteComment(
      @PathVariable("commentId") UUID commentId
  ) {
    commentService.hardDeleteComment(commentId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<CursorPageResponseCommentDto> findAllComments(
      @RequestParam UUID articleId,
      @RequestParam String orderBy,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam int limit,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId
  ) {
    return ResponseEntity.ok(
        commentService.findAll(articleId, orderBy, cursor, after, limit, requestUserId)
    );
  }

  @PostMapping("/{commentId}/comment-likes")
  public ResponseEntity<CommentLikeDto> likeComment(
      @PathVariable("commentId") UUID commentId,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId
  ) {
    return ResponseEntity.ok(commentService.likeComment(commentId, requestUserId));
  }

  @DeleteMapping("/{commentId}/comment-likes")
  public ResponseEntity<Void> unlikeComment(
      @PathVariable("commentId") UUID commentId,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId
  ) {
    commentService.unlikeComment(commentId, requestUserId);
    return ResponseEntity.ok().build();
  }
}

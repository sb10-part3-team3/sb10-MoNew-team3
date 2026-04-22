package com.team3.monew.controller.api;

import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import com.team3.monew.dto.comment.CommentUpdateRequest;
import com.team3.monew.dto.comment.CursorPageResponseCommentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "댓글 관리", description = "댓글 관리 API")
public interface CommentApi {

  String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";

  @Operation(summary = "댓글 등록", description = "새로운 댓글을 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "등록 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(입력값 검증 실패)"),
      @ApiResponse(responseCode = "404", description = "기사 또는 사용자 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<CommentDto> registerComment(
      @Valid @RequestBody CommentRegisterRequest commentRegisterRequest
  );

  @Operation(summary = "댓글 수정", description = "기존 댓글을 수정합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "수정 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(입력값 검증 실패)"),
      @ApiResponse(responseCode = "403", description = "댓글 수정 권한 없음"),
      @ApiResponse(responseCode = "404", description = "댓글 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<CommentDto> updateComment(
      @PathVariable UUID commentId,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId,
      @Valid @RequestBody CommentUpdateRequest commentUpdateRequest
  );

  @Operation(summary = "댓글 논리 삭제", description = "기존 댓글을 논리 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(헤더 누락)"),
      @ApiResponse(responseCode = "403", description = "댓글 삭제 권한 없음"),
      @ApiResponse(responseCode = "404", description = "댓글 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<Void> deleteComment(
      @PathVariable UUID commentId,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId
  );

  @Operation(summary = "댓글 물리 삭제", description = "기존 댓글을 물리 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "404", description = "댓글 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<Void> hardDeleteComment(
      @PathVariable UUID commentId
  );

  @Operation(summary = "댓글 목록 조회", description = "기사별 댓글 목록을 정렬 조건에 따라 커서 기반으로 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(헤더 누락, 정렬 기준 오류, 페이지네이션 파라미터 오류 등)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<CursorPageResponseCommentDto> findAllComments(
      @RequestParam(required = false) UUID articleId,
      @RequestParam String orderBy,
      @RequestParam String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam int limit,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId
  );
}

package com.team3.monew.controller.api;

import com.team3.monew.dto.comment.CommentDto;
import com.team3.monew.dto.comment.CommentRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "댓글 관리", description = "댓글 관리 API")
public interface CommentApi {

  @Operation(summary = "댓글 등록", description = "새로운 댓글을 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "등록 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(입력값 검증 실패)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<CommentDto> registerComment(
      @Parameter(hidden = true) Principal principal,
      @Valid @RequestBody CommentRegisterRequest commentRegisterRequest
  );
}

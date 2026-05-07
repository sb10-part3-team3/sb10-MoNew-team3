package com.team3.monew.controller.api;

import com.team3.monew.dto.article.ArticleDto;
import com.team3.monew.dto.article.ArticleRestoreResultDto;
import com.team3.monew.dto.article.ArticleSearchRequest;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "뉴스 기사 관리", description = "뉴스 기사 관리 API")
public interface ArticleApi {

  String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";

  @Operation(summary = "뉴스 기사 목록 조회", description = "조건에 맞는 뉴스 기사 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (정렬 기준 오류, 페이지네이션 파라미터 오류 등)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CursorPageResponseDto<ArticleDto>> getArticleList(
      @ParameterObject @Valid ArticleSearchRequest searchRequest,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId
  );

  @Operation(summary = "뉴스 기사 단건 조회", description = "조건에 맞는 뉴스 기사를 단건 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (헤더 누락, articleId 형식 오류, 삭제된 기사 조회 등)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "해당 ID에 맞는 뉴스 기사가 존재하지 않음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<ArticleDto> getArticle(
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId,
      @PathVariable UUID articleId
  );

  @Operation(summary = "뉴스 기사 출처 목록 조회", description = "뉴스 기사 출처 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/sources")
  ResponseEntity<List<String>> getArticleSources();

  @Operation(summary = "뉴스 기사 논리 삭제", description = "뉴스 기사를 논리적으로 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "논리 삭제 성공"),
      @ApiResponse(responseCode = "404", description = "뉴스 기사 정보 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> deleteArticle(@PathVariable UUID articleId);

  @Operation(summary = "뉴스 기사 물리 삭제", description = "뉴스 기사를 물리적으로 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "삭제 성공"),
      @ApiResponse(responseCode = "404", description = "뉴스 기사 정보 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<Void> hardDeleteArticle(@PathVariable UUID articleId);

  @Operation(summary = "뉴스 복구", description = "요청 기간의 뉴스 기사를 복구합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "복구 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(from, to 형식 오류)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<List<ArticleRestoreResultDto>> restoreArticle(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to);
}

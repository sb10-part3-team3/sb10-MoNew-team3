package com.team3.monew.controller.api;

import com.team3.monew.dto.article.ArticleViewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "기사 관리", description = "기사 관리 API")
public interface ArticleViewApi {

  String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";

  @Operation(summary = "기사 뷰 등록", description = "기사 조회 이력을 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "기사 뷰 등록 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(헤더 누락, 삭제된 기사 또는 사용자)"),
      @ApiResponse(responseCode = "404", description = "기사 또는 사용자 정보를 찾을 수 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<ArticleViewDto> registerArticleView(
      @PathVariable UUID articleId,
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId
  );
}

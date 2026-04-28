package com.team3.monew.controller.api;

import com.team3.monew.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "뉴스 기사 출처 관리", description = "뉴스 기사 출처 관련 API")
@Validated
public interface ArticleSourceApi {

  @Operation(summary = "뉴스 기사 출처 목록 조회", description = "뉴스 기사 출처 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping("/sources")
  ResponseEntity<List<String>> getArticleSources();
}

package com.team3.monew.controller.api;

import com.team3.monew.dto.notification.NotificationDto;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "알림 관리", description = "알림 관련 API")
@Validated
public interface NotificationApi {

  String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";

  @Operation(summary = "알림 목록 조회", description = "알림 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락 등)",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @GetMapping
  ResponseEntity<CursorPageResponseDto<NotificationDto>> findAllNotConfirmed(
      @RequestHeader(REQUEST_USER_ID_HEADER) UUID requestUserId,
      @RequestParam(required = false) UUID cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam(defaultValue = "50") @Min(1) @Max(100) Integer limit
  );

}

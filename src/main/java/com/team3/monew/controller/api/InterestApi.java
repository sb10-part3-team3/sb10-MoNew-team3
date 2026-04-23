package com.team3.monew.controller.api;

import com.team3.monew.dto.interest.CursorPageResponseInterestDto;
import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.dto.interest.InterestUpdateRequest;
import com.team3.monew.dto.interest.SubscriptionDto;
import com.team3.monew.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Interest", description = "관심사 API")
@RequestMapping("/api/interests")
public interface InterestApi {

  @Operation(
      summary = "관심사 등록",
      description = "관심사 이름과 키워드를 입력받아 새로운 관심사를 등록합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "관심사 등록 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InterestDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청값입니다. (예: 키워드 없음, 형식 오류)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "중복되거나 유사한 관심사 이름입니다.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  @PostMapping
  ResponseEntity<InterestDto> create(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "등록할 관심사 정보",
          required = true,
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InterestRegisterRequest.class)
          )
      )
      @RequestBody @Valid InterestRegisterRequest dto
  );

  @Operation(
      summary = "관심사 키워드 수정",
      description = "관심사의 키워드를 수정합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "수정 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InterestDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "해당 관심사를 찾을 수 없습니다.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  @PatchMapping("/{interestId}")
  ResponseEntity<InterestDto> update(
      @Parameter(
          description = "요청 사용자 ID",
          required = true
      )
      @RequestHeader("Monew-Request-User-Id") UUID userId,
      @PathVariable UUID interestId,
      @RequestBody @Valid InterestUpdateRequest dto
  );

  @Operation(
      summary = "관심사 삭제",
      description = "관심사를 삭제합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "204",
          description = "삭제 성공"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "해당 관심사를 찾을 수 없습니다.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  @DeleteMapping("/{interestId}")
  ResponseEntity<Void> delete(
      @Parameter(
          description = "삭제할 관심사 ID",
          required = true
      )
      @PathVariable UUID interestId
  );

  @GetMapping
  @Operation(
      summary = "관심사 목록 조회",
      description = "관심사를 검색하고 정렬 및 커서 기반 페이지네이션으로 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "조회 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CursorPageResponseInterestDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청값입니다. (예: 정렬 조건/페이지 크기 오류)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  ResponseEntity<CursorPageResponseInterestDto> findAll(
      @Parameter(description = "요청 사용자 ID", required = true)
      @RequestHeader("Monew-Request-User-Id") UUID userId,

      @Parameter(description = "검색어 (관심사 이름 또는 키워드)")
      @RequestParam(value = "keyword", required = false) String keyword,

      @Parameter(description = "정렬 기준 (name, subscriberCount)")
      @RequestParam(value = "orderBy", defaultValue = "name") String orderBy,

      @Parameter(description = "정렬 방향 (ASC, DESC)")
      @RequestParam(value = "direction", defaultValue = "ASC") String direction,

      @Parameter(description = "다음 페이지 커서 값")
      @RequestParam(value = "cursor", required = false) String cursor,

      @Parameter(description = "다음 페이지 기준 시간")
      @RequestParam(value = "after", required = false) Instant after,

      @Parameter(description = "페이지 크기", example = "10")
      @RequestParam(value = "limit", defaultValue = "10") @Min(1) int limit
  );

  @Operation(
      summary = "관심사 구독",
      description = "사용자 ID와 관심사 ID를 통해 특정 관심사를 구독합니다. 구독한 관심사와 관련된 뉴스 기사가 등록되면 알림을 수신할 수 있습니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "구독 성공",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SubscriptionDto.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "해당 사용자 또는 관심사를 찾을 수 없습니다.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "이미 구독 중인 관심사입니다.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 내부 오류",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  @PostMapping("/{interestId}/subscriptions")
  ResponseEntity<SubscriptionDto> subscribe(
      @Parameter(
          description = "요청 사용자 ID",
          required = true
      )
      @RequestHeader("Monew-Request-User-Id") UUID userId,

      @Parameter(
          description = "구독할 관심사 ID",
          required = true
      )
      @PathVariable UUID interestId
  );
}
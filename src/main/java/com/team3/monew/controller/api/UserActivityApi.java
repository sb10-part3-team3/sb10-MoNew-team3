package com.team3.monew.controller.api;

import com.team3.monew.dto.useractivity.UserActivityDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "사용자 활동 내역 관리", description = "사용자 활동 내역 관련 API")
public interface UserActivityApi {

  @Operation(summary = "사용자 활동 내역 조회", description = "사용자 ID로 활동 내역을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "사용자 활동 내역 조회 성공"),
      @ApiResponse(responseCode = "404", description = "사용자 정보 없음"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<UserActivityDto> findUserActivity(UUID userId);
}

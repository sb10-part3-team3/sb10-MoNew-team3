package com.team3.monew.controller.api;

import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
}
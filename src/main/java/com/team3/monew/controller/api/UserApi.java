package com.team3.monew.controller.api;

import com.team3.monew.dto.user.UserDto;
import com.team3.monew.dto.user.UserLoginRequest;
import com.team3.monew.dto.user.UserRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "사용자 관리", description = "사용자 관련 API")
public interface UserApi {

  @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "회원가입 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(입력값 검증 실패)"),
      @ApiResponse(responseCode = "409", description = "이메일 중복"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserRegisterRequest userRegisterRequest);

  @Operation(summary = "로그인", description = "사용자 로그인을 처리합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "로그인 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청(입력값 검증 실패)"),
      @ApiResponse(responseCode = "401", description = "로그인 실패(이메일 또는 비밀번호 불일치)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  ResponseEntity<UserDto> loginUser(@Valid @RequestBody UserLoginRequest userLoginRequest);

}

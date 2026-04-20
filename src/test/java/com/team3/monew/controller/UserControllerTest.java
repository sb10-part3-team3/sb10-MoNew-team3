package com.team3.monew.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.user.UserDto;
import com.team3.monew.dto.user.UserLoginRequest;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.exception.user.AuthException;
import com.team3.monew.exception.user.DuplicateEmailException;
import com.team3.monew.global.exception.GlobalExceptionHandler;
import com.team3.monew.service.UserService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Tag("unit")
@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @Test
  @DisplayName("유효한 요청이면 201과 사용자 정보를 반환한다")
  void shouldRegisterUser_whenRequestIsValid() throws Exception {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "user@example.com",
        "nickname",
        "password123!"
    );

    UserDto response = new UserDto(
        UUID.randomUUID(),
        "user@example.com",
        "nickname",
        Instant.now()
    );

    given(userService.registerUser(any(UserRegisterRequest.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(jsonPath("$.nickname").value("nickname"));
  }

  @Test
  @DisplayName("이메일이 비어있으면 400을 반환한다")
  void shouldReturnBadRequest_whenEmailIsBlank() throws Exception {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "",
        "nickname",
        "password123!"
    );

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("이메일 형식이 아니면 400을 반환한다")
  void shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "invalid-email",
        "nickname",
        "password123!"
    );

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("비밀번호 규칙을 만족하지 않으면 400을 반환한다")
  void shouldReturnBadRequest_whenPasswordInvalid() throws Exception {
    // given (영문/숫자 조건 위반)
    UserRegisterRequest request = new UserRegisterRequest(
        "user@example.com",
        "nickname",
        "aaaaaaaa12"
    );

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("중복 이메일이면 409를 반환한다")
  void shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "user@example.com",
        "nickname",
        "password123!"
    );

    given(userService.registerUser(any(UserRegisterRequest.class)))
        .willThrow(new DuplicateEmailException());

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("유효한 로그인 요청이면 200과 사용자 정보 및 토큰 헤더를 반환한다")
  void shouldLoginUser_whenRequestIsValid() throws Exception {
    // given
    UserLoginRequest request = new UserLoginRequest(
        "user@example.com",
        "password123!"
    );

    UserDto response = new UserDto(
        UUID.randomUUID(),
        "user@example.com",
        "nickname",
        Instant.now()
    );

    given(userService.loginUser(any(UserLoginRequest.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        // 커스텀 응답 헤더 검증
        .andExpect(header().exists("monew-request-id"))
        .andExpect(header().string("monew-request-id", response.id().toString()))
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(jsonPath("$.nickname").value("nickname"));
  }

  @Test
  @DisplayName("로그인 이메일 형식이 올바르지 않으면 400을 반환한다")
  void shouldReturnBadRequest_whenLoginEmailIsInvalid() throws Exception {
    // given
    UserLoginRequest request = new UserLoginRequest(
        "invalid-email",
        "password123!"
    );

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("로그인 비밀번호 형식이 올바르지 않으면 400을 반환한다")
  void shouldReturnBadRequest_whenLoginPasswordIsInvalid() throws Exception {
    // given
    UserLoginRequest request = new UserLoginRequest(
        "user@example.com",
        "aaaaaaa123" // 예: 특수문자 미포함
    );

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("존재하지 않는 이메일로 로그인하면 401을 반환한다")
  void shouldReturnUnauthorized_whenLoginEmailNotFound() throws Exception {
    // given
    UserLoginRequest request = new UserLoginRequest(
        "user@example.com",
        "password123!"
    );

    given(userService.loginUser(any(UserLoginRequest.class)))
        .willThrow(new AuthException());

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("비밀번호가 일치하지 않으면 401을 반환한다")
  void shouldReturnUnauthorized_whenPasswordMismatch() throws Exception {
    // given
    UserLoginRequest request = new UserLoginRequest(
        "user@example.com",
        "wrongPassword123!"
    );

    given(userService.loginUser(any(UserLoginRequest.class)))
        .willThrow(new AuthException());

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
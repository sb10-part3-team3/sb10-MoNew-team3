package com.team3.monew.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.user.UserDto;
import com.team3.monew.dto.user.UserLoginRequest;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.dto.user.UserUpdateRequest;
import com.team3.monew.exception.user.AuthException;
import com.team3.monew.exception.user.DeletedUserException;
import com.team3.monew.exception.user.DuplicateEmailException;
import com.team3.monew.exception.user.UserNotFoundException;
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

  @Test
  @DisplayName("유효한 요청으로 사용자 수정 시 200 OK와 UserDto를 반환한다")
  void shouldReturnUserDto_whenValidRequest() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.now();

    UserUpdateRequest request = new UserUpdateRequest("newname");
    UserDto response = new UserDto(
        userId,
        "email1@naver.com",
        "newname",
        createdAt
    );

    given(userService.updateUser(any(UUID.class), any(UserUpdateRequest.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(patch("/api/users/{userId}", userId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.email").value("email1@naver.com"))
        .andExpect(jsonPath("$.nickname").value("newname"))
        .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));
  }

  @Test
  @DisplayName("닉네임이 10자를 초과하면 400 Bad Request를 반환한다")
  void shouldReturnBadRequest_whenNicknameTooLong() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("newUsername"); // 11자

    // when & then
    mockMvc.perform(patch("/api/users/{userId}", userId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("잘못된 UUID 형식의 path variable이면 400 Bad Request를 반환한다")
  void shouldReturnBadRequest_whenInvalidPathVariableUuid() throws Exception {
    // given
    UserUpdateRequest request = new UserUpdateRequest("newname");

    // when & then
    mockMvc.perform(patch("/api/users/{userId}", "invalid-uuid")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("논리 삭제 요청이 유효하면 200을 반환한다.")
  void shouldDeleteUser_whenRequestIsValid() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    willDoNothing().given(userService).deleteUser(userId);

    // when & then
    mockMvc.perform(delete("/api/users/{userId}", userId))
        .andExpect(status().isOk());

    verify(userService).deleteUser(userId);
  }

  @Test
  @DisplayName("논리 삭제 시 사용자가 없으면 예외가 발생한다.")
  void shouldThrowException_whenUserNotFoundOnSoftDelete() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    willThrow(new UserNotFoundException(userId))
        .given(userService).deleteUser(userId);

    // when & then
    mockMvc.perform(delete("/api/users/{userId}", userId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("논리 삭제 시 이미 삭제된 사용자면 예외가 발생한다.")
  void shouldThrowException_whenUserAlreadyDeletedOnSoftDelete() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    willThrow(new DeletedUserException(userId))
        .given(userService).deleteUser(userId);

    // when & then
    mockMvc.perform(delete("/api/users/{userId}", userId))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("물리 삭제 요청이 유효하면 200을 반환한다.")
  void shouldHardDeleteUser_whenRequestIsValid() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    willDoNothing().given(userService).hardDeleteUser(userId);

    // when & then
    mockMvc.perform(delete("/api/users/{userId}/hard", userId))
        .andExpect(status().isOk());

    verify(userService).hardDeleteUser(userId);
  }

  @Test
  @DisplayName("물리 삭제 시 사용자가 없으면 예외가 발생한다.")
  void shouldThrowException_whenUserNotFoundOnHardDelete() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    willThrow(new UserNotFoundException(userId))
        .given(userService).hardDeleteUser(userId);

    // when & then
    mockMvc.perform(delete("/api/users/{userId}/hard", userId))
        .andExpect(status().isNotFound());
  }
}
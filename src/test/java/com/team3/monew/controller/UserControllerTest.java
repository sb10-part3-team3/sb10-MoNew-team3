package com.team3.monew.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.user.UserDto;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.exception.user.DuplicateEmailException;
import com.team3.monew.global.exception.GlobalExceptionHandler;
import com.team3.monew.service.UserService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Tag("unit")
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
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
        "password123"
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
        "password123"
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
        "password123"
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
        "aaaaaaaa"
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
        "password123"
    );

    given(userService.registerUser(any(UserRegisterRequest.class)))
        .willThrow(new DuplicateEmailException("user@example.com"));

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }
}
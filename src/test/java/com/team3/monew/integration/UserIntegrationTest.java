package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.user.UserLoginRequest;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.dto.user.UserUpdateRequest;
import com.team3.monew.entity.User;
import com.team3.monew.repository.UserRepository;
import com.team3.monew.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class UserIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("유효한 요청이면 회원가입에 성공한다")
  void shouldRegisterUser_whenRequestIsValid() throws Exception {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "user@example.com",
        "nickname",
        "password123!"
    );

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(jsonPath("$.nickname").value("nickname"));
  }

  @Test
  @DisplayName("중복 이메일이면 409를 반환한다")
  void shouldReturnConflict_whenEmailAlreadyExists() throws Exception {
    // given
    UserRegisterRequest firstRequest = new UserRegisterRequest(
        "duplicate@example.com",
        "nickname1",
        "password123!"
    );

    UserRegisterRequest secondRequest = new UserRegisterRequest(
        "duplicate@example.com",
        "nickname2",
        "password456!"
    );

    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(firstRequest)))
        .andExpect(status().isCreated());

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(secondRequest)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("잘못된 이메일 형식이면 400을 반환한다")
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
  @DisplayName("유효한 로그인 요청이면 200과 사용자 정보를 반환한다.")
  void shouldLoginUser_whenRequestIsValid() throws Exception {
    // given
    UserRegisterRequest registerRequest = new UserRegisterRequest(
        "login-success@example.com",
        "nickname",
        "password123!"
    );

    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isCreated());

    UserLoginRequest loginRequest = new UserLoginRequest(
        "login-success@example.com",
        "password123!"
    );

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("login-success@example.com"))
        .andExpect(jsonPath("$.nickname").value("nickname"));
  }

  @Test
  @DisplayName("로그인 이메일 형식이 잘못되면 400을 반환한다")
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
        "aaaaaaa123"
    );

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("이메일 또는 비밀번호가 올바르지 않으면 401을 반환한다")
  void shouldReturnUnauthorized_whenLoginCredentialsAreInvalid() throws Exception {
    // given
    UserRegisterRequest registerRequest = new UserRegisterRequest(
        "login-fail@example.com",
        "nickname",
        "password123!"
    );

    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registerRequest)))
        .andExpect(status().isCreated());

    UserLoginRequest loginRequest = new UserLoginRequest(
        "login-fail@example.com",
        "wrongPassword123!"
    );

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("유효한 요청이면 사용자 수정에 성공한다")
  void shouldUpdateUser_whenRequestIsValid() throws Exception {
    // given
    User savedUser = userRepository.save(
        User.create("update-success@example.com", "oldNick", "password123!")
    );

    UserUpdateRequest request = new UserUpdateRequest("newNick");

    // when & then
    mockMvc.perform(patch("/api/users/{userId}", savedUser.getId())
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("update-success@example.com"))
        .andExpect(jsonPath("$.nickname").value("newNick"));
  }

  @Test
  @DisplayName("존재하지 않는 사용자면 404를 반환한다")
  void shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
    // given
    UUID invalidUserId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("newNick");

    // when & then
    mockMvc.perform(patch("/api/users/{userId}", invalidUserId)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("유효한 요청이면 논리 삭제에 성공한다.")
  void shouldSoftDeleteUser_whenRequestIsValid() throws Exception {
    // given
    User savedUser = userRepository.save(
        User.create("soft-delete@example.com", "nickname", "password123!")
    );

    // when & then
    mockMvc.perform(delete("/api/users/{userId}", savedUser.getId()))
        .andExpect(status().isOk());

    User deletedUser = userRepository.findById(savedUser.getId()).orElseThrow();
    assertThat(deletedUser.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("유효한 요청이면 물리 삭제에 성공한다.")
  void shouldHardDeleteUser_whenRequestIsValid() throws Exception {
    // given
    User savedUser = userRepository.save(
        User.create("hard-delete@example.com", "nickname", "password123!")
    );

    // when & then
    mockMvc.perform(delete("/api/users/{userId}/hard", savedUser.getId()))
        .andExpect(status().isOk());

    assertThat(userRepository.findById(savedUser.getId())).isEmpty();
  }
}

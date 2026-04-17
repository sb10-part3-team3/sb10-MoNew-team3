package com.team3.monew.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.repository.UserRepository;
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
class UserIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("유효한 요청이면 회원가입에 성공한다")
  void shouldCreateUser_whenRequestIsValid() throws Exception {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "user@example.com",
        "nickname",
        "password123"
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
        "password123"
    );

    UserRegisterRequest secondRequest = new UserRegisterRequest(
        "duplicate@example.com",
        "nickname2",
        "password456"
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
        "password123"
    );

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}

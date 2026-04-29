package com.team3.monew.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.monew.document.UserActivityDocument;
import com.team3.monew.repository.UserActivityRepository;
import com.team3.monew.support.IntegrationTestSupport;
import java.time.Instant;
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
class UserActivityIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserActivityRepository userActivityRepository;

  @Test
  @DisplayName("사용자 활동 내역 조회 API 통합 테스트에 성공합니다.")
  void shouldFindUserActivityIntegration() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-04-24T00:00:00Z");

    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    userActivityRepository.save(document);

    // when & then
    mockMvc.perform(get("/api/user-activities/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.email").value("test@test.com"))
        .andExpect(jsonPath("$.nickname").value("tester"));
  }
}
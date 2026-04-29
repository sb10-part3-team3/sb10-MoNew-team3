package com.team3.monew.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.monew.dto.useractivity.UserActivityDto;
import com.team3.monew.service.UserActivityService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserActivityController.class)
class UserActivityControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserActivityService userActivityService;

  @Test
  @DisplayName("사용자 활동 내역 조회에 성공합니다.")
  void shouldFindUserActivity() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    UserActivityDto response = new UserActivityDto(
        userId,
        "test@test.com",
        "tester",
        Instant.parse("2026-04-24T00:00:00Z"),
        List.of(),
        List.of(),
        List.of(),
        List.of()
    );

    given(userActivityService.findUserActivity(eq(userId))).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/user-activities/{userId}", userId))
        .andExpect(status().isOk());

    then(userActivityService).should(times(1)).findUserActivity(userId);
  }
}
package com.team3.monew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.exception.interest.InterestDuplicateNameException;
import com.team3.monew.service.InterestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterestController.class)
@Tag("unit")
class InterestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objMapper;

  @MockitoBean
  private InterestService interestService;

  @Test
  @DisplayName("내가 구독하지 않은 상태인 관심사를 등록할 수 있다")
  void shouldRegisterInterest_whenInterestNameDoesntDuplicate() throws Exception {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "주식",
        List.of("코스피", "삼성전자")
    );

    InterestDto response = new InterestDto(
        UUID.randomUUID(),
        "주식",
        List.of("코스피", "삼성전자"),
        0,
        false
    );

    given(interestService.create(any(InterestRegisterRequest.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("주식"))
        .andExpect(jsonPath("$.subscriberCount").value(0))
        .andExpect(jsonPath("$.keywords[0]").value("코스피"))
        .andExpect(jsonPath("$.keywords[1]").value("삼성전자"));
  }

  @Test
  @DisplayName("중복된 관심사 이름이면 등록에 실패한다")
  void shouldFailToRegister_whenDuplicateNameExists() throws Exception {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "test",
        List.of("keyword", "test")
    );

    given(interestService.create(any(InterestRegisterRequest.class)))
        .willThrow(new InterestDuplicateNameException());

    // when & then
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("관심사 키워드가 비어 있으면 관심사 등록에 실패한다")
  void shouldFailToRegister_whenKeywordsAreEmpty() throws Exception {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest("noKeyword", List.of());

    // when & then
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
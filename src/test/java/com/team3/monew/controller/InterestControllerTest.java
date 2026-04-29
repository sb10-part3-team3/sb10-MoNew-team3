package com.team3.monew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.dto.interest.InterestUpdateRequest;
import com.team3.monew.dto.interest.SubscriptionDto;
import com.team3.monew.dto.interest.internal.InterestCursor;
import com.team3.monew.dto.interest.internal.InterestSearchCondition;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.exception.interest.InterestDuplicateNameException;
import com.team3.monew.exception.interest.InterestException;
import com.team3.monew.exception.interest.InterestNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.service.InterestService;
import java.time.Instant;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

  private static final String REQUEST_USER_HEADER = "Monew-Request-User-Id";

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

    given(interestService.createInterest(any(InterestRegisterRequest.class)))
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

    given(interestService.createInterest(any(InterestRegisterRequest.class)))
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

  @Test
  @DisplayName("유사한 관심사 이름이면 400 응답을 반환한다")
  void shouldReturnConflict_whenInterestNameIsSimilar() throws Exception {
    // given
    given(interestService.createInterest(any()))
        .willThrow(new InterestDuplicateNameException());

    String requestJson = """
        {
          "name": "삼셩전자",
          "keywords": ["키워드"]
        }
        """;

    // when & then
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("관심사 키워드를 수정할 수 있다")
  void shouldUpdateInterestKeywords_whenValidRequest() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(
        List.of("나스닥", "애플")
    );

    InterestDto response = new InterestDto(
        interestId,
        "주식",
        List.of("나스닥", "애플"),
        0,
        true
    );

    given(interestService.updateKeyword(
        eq(interestId),
        any(InterestUpdateRequest.class)
    )).willReturn(response);

    // when & then
    mockMvc.perform(patch("/api/interests/{interestId}", interestId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(interestId.toString()))
        .andExpect(jsonPath("$.name").value("주식"))
        .andExpect(jsonPath("$.keywords[0]").value("나스닥"))
        .andExpect(jsonPath("$.keywords[1]").value("애플"))
        .andExpect(jsonPath("$.subscriberCount").value(0));
  }

  @Test
  @DisplayName("관심사 등록 요청 본문이 없으면 실패한다")
  void shouldFailCreate_whenRequestBodyMissing() throws Exception {
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("관심사 수정 요청 본문이 없으면 실패한다")
  void shouldFailUpdate_whenRequestBodyMissing() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    mockMvc.perform(patch("/api/interests/{interestId}", interestId)
            .header(REQUEST_USER_HEADER, userId.toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("관심사를 삭제할 수 있다")
  void shouldDeleteInterest_whenDeleteRequest() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();

    doNothing().when(interestService).deleteInterest(interestId);

    // when & then
    mockMvc.perform(delete("/api/interests/{interestId}", interestId))
        .andExpect(status().isNoContent());

    then(interestService).should().deleteInterest(interestId);
  }

  @Test
  @DisplayName("존재하지 않는 관심사를 삭제하면 404를 반환한다")
  void shouldReturn404_whenInterestNotFound() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();

    doThrow(new InterestNotFoundException())
        .when(interestService).deleteInterest(interestId);

    // when & then
    mockMvc.perform(delete("/api/interests/{interestId}", interestId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("관심사 목록을 조회할 수 있다")
  void shouldFindAllInterests_whenRequestIsValid() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    Instant after = Instant.parse("2026-04-22T10:00:00Z");

    InterestDto first = new InterestDto(
        UUID.randomUUID(),
        "가구",
        List.of("의자", "책상"),
        3,
        true
    );

    InterestDto second = new InterestDto(
        UUID.randomUUID(),
        "나무",
        List.of("소나무"),
        1,
        false
    );

    CursorPageResponseDto<InterestDto> response = new CursorPageResponseDto<InterestDto>(
        List.of(first, second),
        "나무",
        after,
        2,
        5L,
        true
    );

    InterestSearchCondition condition = new InterestSearchCondition(
        "구",
        "name",
        "ASC",
        new InterestCursor(null, null),
        2
    );

    given(interestService.findAll(condition, userId)).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/interests")
            .header(REQUEST_USER_HEADER, userId.toString())
            .param("keyword", "구")
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].name").value("가구"))
        .andExpect(jsonPath("$.content[0].subscribedByMe").value(true))
        .andExpect(jsonPath("$.content[1].name").value("나무"))
        .andExpect(jsonPath("$.content[1].subscribedByMe").value(false))
        .andExpect(jsonPath("$.nextCursor").value("나무"))
        .andExpect(jsonPath("$.nextAfter").value(after.toString()))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(5))
        .andExpect(jsonPath("$.hasNext").value(true));

    then(interestService).should().findAll(condition, userId);
  }

  @Test
  @DisplayName("커서 없이 첫 페이지를 조회할 수 있다")
  void shouldFindFirstPage_whenCursorAndAfterAreMissing() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    CursorPageResponseDto<InterestDto> response = new CursorPageResponseDto<InterestDto>(
        List.of(),
        null,
        null,
        10,
        0L,
        false
    );

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    given(interestService.findAll(condition, userId)).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/interests")
            .header(REQUEST_USER_HEADER, userId.toString())
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0))
        .andExpect(jsonPath("$.nextCursor").doesNotExist())
        .andExpect(jsonPath("$.nextAfter").doesNotExist())
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.hasNext").value(false));

    then(interestService).should().findAll(condition, userId);
  }

  @Test
  @DisplayName("커서와 after 값으로 다음 페이지를 조회할 수 있다")
  void shouldFindNextPage_whenCursorAndAfterAreGiven() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    Instant after = Instant.parse("2026-04-22T10:00:00Z");

    CursorPageResponseDto<InterestDto> response = new CursorPageResponseDto<InterestDto>(
        List.of(),
        "다리",
        after,
        2,
        5L,
        true
    );

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor("나무", after),
        2
    );

    given(interestService.findAll(condition, userId)).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/interests")
            .header(REQUEST_USER_HEADER, userId.toString())
            .param("orderBy", "name")
            .param("direction", "ASC")
            .param("cursor", "나무")
            .param("after", after.toString())
            .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nextCursor").value("다리"))
        .andExpect(jsonPath("$.hasNext").value(true));

    then(interestService).should().findAll(condition, userId);
  }

  @Test
  @DisplayName("관심사 등록 요청이 올바르지 않으면 400을 반환한다")
  void shouldReturnBadRequest_whenCreateRequestIsInvalid() throws Exception {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "",
        List.of()
    );

    // when & then
    mockMvc.perform(post("/api/interests")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    then(interestService).should(never()).createInterest(request);
  }

  @Test
  @DisplayName("관심사를 구독할 수 있다")
  void shouldSubscribeInterest_whenSubscribeRequest() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();

    SubscriptionDto response = new SubscriptionDto(
        subscriptionId,
        interestId,
        "주식",
        List.of("코스피", "삼성전자"),
        1,
        Instant.parse("2026-04-23T12:00:00Z")
    );

    given(interestService.subscribe(userId, interestId)).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId)
            .header(REQUEST_USER_HEADER, userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(subscriptionId.toString()))
        .andExpect(jsonPath("$.interestId").value(interestId.toString()))
        .andExpect(jsonPath("$.interestName").value("주식"))
        .andExpect(jsonPath("$.interestKeywords[0]").value("코스피"))
        .andExpect(jsonPath("$.interestKeywords[1]").value("삼성전자"))
        .andExpect(jsonPath("$.interestSubscriberCount").value(1))
        .andExpect(jsonPath("$.createdAt").value("2026-04-23T12:00:00Z"));
  }

  @Test
  @DisplayName("존재하지 않는 사용자는 관심사를 구독할 수 없다")
  void shouldFailToSubscribe_whenUserNotFound() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    given(interestService.subscribe(userId, interestId))
        .willThrow(new UserNotFoundException(userId));

    // when & then
    mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId)
            .header(REQUEST_USER_HEADER, userId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("존재하지 않는 관심사는 구독할 수 없다")
  void shouldFailToSubscribe_whenInterestNotFound() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    given(interestService.subscribe(userId, interestId))
        .willThrow(new InterestNotFoundException());

    // when & then
    mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId)
            .header(REQUEST_USER_HEADER, userId))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("이미 구독 중인 관심사는 다시 구독할 수 없다")
  void shouldFailToSubscribe_whenAlreadySubscribing() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    given(interestService.subscribe(userId, interestId))
        .willThrow(new InterestException(ErrorCode.INTEREST_ALREADY_SUBSCRIBING));

    // when & then
    mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId)
            .header(REQUEST_USER_HEADER, userId))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("요청 헤더의 사용자 ID가 없으면 관심사를 구독할 수 없다")
  void shouldFailToSubscribe_whenUserIdHeaderIsMissing() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();

    // when & then
    mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("잘못된 형식의 사용자 ID 헤더로는 관심사를 구독할 수 없다")
  void shouldFailToSubscribe_whenUserIdHeaderIsInvalid() throws Exception {
    // given
    UUID interestId = UUID.randomUUID();

    // when & then
    mockMvc.perform(post("/api/interests/{interestId}/subscriptions", interestId)
            .header(REQUEST_USER_HEADER, "invalid-uuid"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("관심사 구독을 취소할 수 있다")
  void shouldCancelSubscribe() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    willDoNothing()
        .given(interestService)
        .cancelSubscribe(userId, interestId);

    // when & then
    mockMvc.perform(delete("/api/interests/{interestId}/subscriptions", interestId)
            .header(REQUEST_USER_HEADER, userId))
        .andExpect(status().isNoContent());

    then(interestService).should()
        .cancelSubscribe(userId, interestId);
  }

  @Test
  @DisplayName("구독하지 않은 관심사는 구독 취소할 수 없다")
  void shouldFailToCancelSubscribe_whenNotSubscribing() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    willThrow(new InterestException(ErrorCode.INTEREST_NOT_SUBSCRIBING))
        .given(interestService)
        .cancelSubscribe(userId, interestId);

    // when & then
    mockMvc.perform(delete("/api/interests/{interestId}/subscriptions", interestId)
            .header(REQUEST_USER_HEADER, userId))
        .andExpect(status().isBadRequest());

    then(interestService).should()
        .cancelSubscribe(userId, interestId);
  }

  @Test
  @DisplayName("존재하지 않는 사용자는 관심사 구독을 취소할 수 없다")
  void shouldFailToCancelSubscribe_whenUserNotFound() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    willThrow(new UserNotFoundException(userId))
        .given(interestService)
        .cancelSubscribe(userId, interestId);

    // when & then
    mockMvc.perform(delete("/api/interests/{interestId}/subscriptions", interestId)
            .header(REQUEST_USER_HEADER, userId))
        .andExpect(status().isNotFound());

    then(interestService).should()
        .cancelSubscribe(userId, interestId);
  }

  @Test
  @DisplayName("존재하지 않는 관심사는 구독을 취소할 수 없다")
  void shouldFailToCancelSubscribe_whenInterestNotFound() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    willThrow(new InterestNotFoundException())
        .given(interestService)
        .cancelSubscribe(userId, interestId);

    // when & then
    mockMvc.perform(delete("/api/interests/{interestId}/subscriptions", interestId)
            .header(REQUEST_USER_HEADER, userId))
        .andExpect(status().isNotFound());

    then(interestService).should()
        .cancelSubscribe(userId, interestId);
  }
}
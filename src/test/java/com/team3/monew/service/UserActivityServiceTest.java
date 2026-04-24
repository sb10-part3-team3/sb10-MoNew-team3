package com.team3.monew.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.team3.monew.document.*;
import com.team3.monew.dto.useractivity.UserActivityDto;
import com.team3.monew.exception.useractivity.UserActivityNotFoundException;
import com.team3.monew.mapper.UserActivityMapper;
import com.team3.monew.repository.UserActivityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

  @Mock
  private UserActivityRepository userActivityRepository;

  @Mock
  private UserActivityMapper userActivityMapper;

  @InjectMocks
  private UserActivityService userActivityService;

  private UUID userId;
  private Instant createdAt;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    createdAt = Instant.parse("2026-04-24T00:00:00Z");
  }

  @Test
  @DisplayName("사용자 활동 조회에 성공합니다.")
  void shouldFindUserActivity() {
    // given
    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );
    UserActivityDto dto = mock(UserActivityDto.class);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));
    given(userActivityMapper.toDto(document)).willReturn(dto);

    // when
    UserActivityDto result = userActivityService.findUserActivity(userId);

    // then
    assertNotNull(result);
    assertEquals(dto, result);

    then(userActivityRepository).should().findById(userId);
    then(userActivityMapper).should().toDto(document);
  }

  @Test
  @DisplayName("사용자 활동 내역 조회 시 문서가 없으면 예외가 발생합니다.")
  void shouldThrowExceptionWhenUserActivityNotFound() {
    // given
    UUID userId = UUID.randomUUID();

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThrows(
        UserActivityNotFoundException.class,
        () -> userActivityService.findUserActivity(userId)
    );

    then(userActivityRepository).should(times(1)).findById(userId);
    then(userActivityMapper).should(never()).toDto(any());
  }

  @Test
  @DisplayName("사용자 등록 이벤트 처리 시 새 사용자 활동 문서 저장에 성공합니다.")
  void shouldRegisterUserActivity() {
    // given
    UserActivityRequest request = new UserActivityRequest(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    UserActivityDocument document = UserActivityDocument.create(
        userId,
        request.email(),
        request.nickname(),
        request.createdAt()
    );

    given(userActivityRepository.findById(request.id())).willReturn(Optional.empty());
    given(userActivityMapper.toDocument(request)).willReturn(document);

    // when
    userActivityService.registerUserActivity(request);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertNotNull(savedDocument);
    assertEquals(userId, savedDocument.getId());
    assertEquals("test@test.com", savedDocument.getEmail());
    assertEquals("tester", savedDocument.getNickname());
    assertEquals(createdAt, savedDocument.getCreatedAt());
  }

  @Test
  @DisplayName("사용자 등록 이벤트 처리 시 기존 임시 문서가 있으면 사용자 정보를 갱신합니다.")
  void shouldUpdateUserInfoWhenUserActivityAlreadyExists() {
    // given
    UserActivityRequest request = new UserActivityRequest(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    UserActivityDocument existingDocument = UserActivityDocument.empty(userId);

    given(userActivityRepository.findById(request.id())).willReturn(Optional.of(existingDocument));

    // when
    userActivityService.registerUserActivity(request);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());
    then(userActivityMapper).should(never()).toDocument(any());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertEquals(userId, savedDocument.getId());
    assertEquals("test@test.com", savedDocument.getEmail());
    assertEquals("tester", savedDocument.getNickname());
    assertEquals(createdAt, savedDocument.getCreatedAt());
  }

  @Test
  @DisplayName("구독 요약 추가에 성공합니다.")
  void shouldUpdateSubscriptionSummary() {
    // given
    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    SubscriptionSummary summary = new SubscriptionSummary(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "경제",
        List.of("금리", "주식"),
        10,
        createdAt
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityService.updateSubscriptionSummary(userId, summary);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertEquals(1, savedDocument.getSubscriptions().size());
    assertEquals(summary, savedDocument.getSubscriptions().get(0));
  }

  @Test
  @DisplayName("댓글 요약 추가에 성공합니다.")
  void shouldUpdateCommentSummary() {
    // given
    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    CommentSummary summary = new CommentSummary(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0,
        createdAt
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityService.updateCommentSummary(summary);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertEquals(1, savedDocument.getComments().size());
    assertEquals(summary, savedDocument.getComments().get(0));
  }

  @Test
  @DisplayName("댓글 좋아요 요약 추가에 성공합니다.")
  void shouldUpdateCommentLikeSummary() {
    // given
    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    CommentLikeSummary summary = new CommentLikeSummary(
        UUID.randomUUID(),
        createdAt,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        UUID.randomUUID(),
        "commentWriter",
        "댓글 내용",
        1,
        createdAt
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityService.updateCommentLikeSummary(userId, summary);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertEquals(1, savedDocument.getCommentLikes().size());
    assertEquals(summary, savedDocument.getCommentLikes().get(0));
  }

  @Test
  @DisplayName("기사 조회 요약 추가에 성공합니다.")
  void shouldUpdateArticleViewSummary() {
    // given
    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    ArticleViewSummary summary = new ArticleViewSummary(
        UUID.randomUUID(),
        userId,
        createdAt,
        UUID.randomUUID(),
        "NAVER",
        "https://example.com",
        "기사 제목",
        createdAt,
        "기사 요약",
        3,
        100
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityService.updateArticleViewSummary(userId, summary);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertEquals(1, savedDocument.getArticleViews().size());
    assertEquals(summary, savedDocument.getArticleViews().get(0));
  }
}
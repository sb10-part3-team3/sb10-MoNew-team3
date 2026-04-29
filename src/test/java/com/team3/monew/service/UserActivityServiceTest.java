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
  @DisplayName("구독 요약 추가 시 문서가 없으면 빈 문서를 생성하고 저장합니다.")
  void shouldCreateEmptyDocumentWhenAddingSubscriptionSummary() {
    // given
    SubscriptionSummary summary = new SubscriptionSummary(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "경제",
        List.of("금리", "주식"),
        10,
        createdAt
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    // when
    userActivityService.updateSubscriptionSummary(userId, summary);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertNull(savedDocument.getEmail());        // 빈 문서라 null
    assertNull(savedDocument.getNickname());     // 빈 문서라 null
    assertEquals(userId, savedDocument.getId());
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
  @DisplayName("댓글 요약 추가 시 문서가 없으면 빈 문서를 생성하고 저장합니다.")
  void shouldCreateEmptyDocumentWhenAddingCommentSummary() {
    // given
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

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    // when
    userActivityService.updateCommentSummary(summary);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertNull(savedDocument.getEmail());
    assertNull(savedDocument.getNickname());
    assertEquals(userId, savedDocument.getId());
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
  @DisplayName("댓글 좋아요 요약 추가 시 문서가 없으면 빈 문서를 생성하고 저장합니다.")
  void shouldCreateEmptyDocumentWhenAddingCommentLikeSummary() {
    // given
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

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    // when
    userActivityService.updateCommentLikeSummary(userId, summary);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertNull(savedDocument.getEmail());
    assertNull(savedDocument.getNickname());
    assertEquals(userId, savedDocument.getId());
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

  @Test
  @DisplayName("기사 조회 요약 추가 시 문서가 없으면 빈 문서를 생성하고 저장합니다.")
  void shouldCreateEmptyDocumentWhenAddingArticleViewSummary() {
    // given
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

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    // when
    userActivityService.updateArticleViewSummary(userId, summary);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertNull(savedDocument.getEmail());
    assertNull(savedDocument.getNickname());
    assertEquals(userId, savedDocument.getId());
    assertEquals(1, savedDocument.getArticleViews().size());
    assertEquals(summary, savedDocument.getArticleViews().get(0));
  }

  @Test
  @DisplayName("사용자 삭제 이벤트 처리 시 활동 내역 문서 삭제에 성공합니다.")
  void shouldDeleteUserActivity() {
    // given
    // when
    userActivityService.deleteUserActivity(userId);

    // then
    then(userActivityRepository).should().deleteById(userId);
  }

  @Test
  @DisplayName("사용자 닉네임 수정 시 활동 내역 문서 닉네임 업데이트에 성공합니다.")
  void shouldUpdateUserNickname() {
    // given
    String newNickname = "newTester";

    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityService.updateUserNickname(userId, newNickname);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertNotNull(savedDocument);
    assertEquals(userId, savedDocument.getId());
    assertEquals(newNickname, savedDocument.getNickname());
  }

  @Test
  @DisplayName("사용자 닉네임 수정 시 본인이 작성한 댓글의 닉네임도 함께 업데이트됩니다.")
  void shouldUpdateNicknameInCommentSummaries() {
    // given
    String newNickname = "newTester";

    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        newNickname,
        createdAt
    );

    CommentSummary comment = new CommentSummary(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0,
        createdAt
    );

    document.addCommentSummary(comment);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));
    given(userActivityRepository.findAllByCommentsUserId(userId)).willReturn(List.of());

    // when
    userActivityService.updateUserNickname(userId, newNickname);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertNotNull(savedDocument);
    assertEquals(userId, savedDocument.getId());
    assertEquals(newNickname, savedDocument.getNickname());
    assertEquals(1, savedDocument.getComments().size());
    assertEquals(newNickname, savedDocument.getComments().get(0).userNickname());
  }

  @Test
  @DisplayName("사용자 닉네임 수정 시 문서가 없으면 예외가 발생합니다.")
  void shouldThrowExceptionWhenUserActivityNotFoundOnUpdateNickname() {
    // given
    String newNickname = "newTester";

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThrows(
        UserActivityNotFoundException.class,
        () -> userActivityService.updateUserNickname(userId, newNickname)
    );

    then(userActivityRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("댓글 삭제 시 활동 내역 댓글 목록에서 해당 댓글을 제거합니다.")
  void shouldRemoveCommentSummary() {
    // given
    UUID commentId = UUID.randomUUID();

    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    CommentSummary summary = new CommentSummary(
        commentId,
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0,
        createdAt
    );

    document.addCommentSummary(summary);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityService.removeCommentSummary(userId, commentId);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertNotNull(savedDocument);
    assertEquals(userId, savedDocument.getId());
    assertEquals(0, savedDocument.getComments().size());
  }

  @Test
  @DisplayName("댓글 삭제 시 다른 유저의 활동 내역 좋아요 목록에서도 해당 댓글의 좋아요를 제거합니다.")
  void shouldRemoveCommentLikeSummaryFromOtherUserActivity_whenCommentDeleted() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();

    UserActivityDocument userActivityDocument = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    CommentSummary commentSummary = new CommentSummary(
        commentId,
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0,
        createdAt
    );

    UserActivityDocument otherUserActivityDocument = UserActivityDocument.create(
        otherUserId,
        "other@test.com",
        "otherTester",
        createdAt
    );

    CommentLikeSummary commentLikeSummary = new CommentLikeSummary(
        UUID.randomUUID(),
        createdAt,
        commentId,         // 삭제될 댓글 ID
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        1,
        createdAt
    );

    userActivityDocument.addCommentSummary(commentSummary);
    otherUserActivityDocument.addCommentLikeSummary(commentLikeSummary);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(userActivityDocument));
    given(userActivityRepository.findAllByCommentLikesCommentId(commentId))
        .willReturn(List.of(otherUserActivityDocument));

    // when
    userActivityService.removeCommentSummary(userId, commentId);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should(times(2)).save(documentCaptor.capture());

    List<UserActivityDocument> savedDocuments = documentCaptor.getAllValues();

    assertEquals(0, savedDocuments.get(0).getComments().size());
    assertEquals(0, savedDocuments.get(1).getCommentLikes().size());
  }

  @Test
  @DisplayName("댓글 삭제 시 문서가 문서가 없으면 정상 종료합니다.")
  void shouldDoNothingWhenUserActivityNotFoundOnRemoveCommentSummary() {
    // given
    UUID commentId = UUID.randomUUID();

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    // when
    userActivityService.removeCommentSummary(userId, commentId);

    // then
    then(userActivityRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("댓글 수정 시 활동 내역 댓글 목록의 내용을 업데이트합니다.")
  void shouldUpdateCommentContent() {
    // given
    UUID commentId = UUID.randomUUID();
    String newContent = "수정된 댓글 내용";

    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    CommentSummary summary = new CommentSummary(
        commentId,
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "기존 댓글 내용",
        0,
        createdAt
    );

    document.addCommentSummary(summary);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityService.updateCommentContent(userId, commentId, newContent);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertNotNull(savedDocument);
    assertEquals(userId, savedDocument.getId());
    assertEquals(1, savedDocument.getComments().size());
    assertEquals(newContent, savedDocument.getComments().get(0).content());
  }

  @Test
  @DisplayName("댓글 수정 시 다른 유저의 활동 내역 좋아요 목록의 댓글 내용도 함께 업데이트됩니다.")
  void shouldUpdateCommentLikeContentFromOtherUserActivity_whenCommentUpdated() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    String newContent = "수정된 댓글 내용";

    UserActivityDocument userActivityDocument = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    CommentSummary commentSummary = new CommentSummary(
        commentId,
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "기존 댓글 내용",
        0,
        createdAt
    );

    UserActivityDocument otherUserActivityDocument = UserActivityDocument.create(
        otherUserId,
        "other@test.com",
        "otherTester",
        createdAt
    );

    CommentLikeSummary commentLikeSummary = new CommentLikeSummary(
        UUID.randomUUID(),
        createdAt,
        commentId,         // 수정될 댓글 ID
        UUID.randomUUID(),
        "기사 제목",
        userId,
        "tester",
        "기존 댓글 내용",
        1,
        createdAt
    );

    userActivityDocument.addCommentSummary(commentSummary);
    otherUserActivityDocument.addCommentLikeSummary(commentLikeSummary);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(userActivityDocument));
    given(userActivityRepository.findAllByCommentLikesCommentId(commentId))
        .willReturn(List.of(otherUserActivityDocument));

    // when
    userActivityService.updateCommentContent(userId, commentId, newContent);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should(times(2)).save(documentCaptor.capture());

    List<UserActivityDocument> savedDocuments = documentCaptor.getAllValues();

    assertEquals(newContent, savedDocuments.get(0).getComments().get(0).content());
    assertEquals(newContent, savedDocuments.get(1).getCommentLikes().get(0).commentContent());
  }

  @Test
  @DisplayName("댓글 수정 시 문서가 문서가 없으면 정상 종료합니다.")
  void shouldDoNothingWhenUserActivityNotFoundOnUpdateCommentContent() {
    // given
    UUID commentId = UUID.randomUUID();
    String newContent = "수정된 댓글 내용";

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    // when
    userActivityService.updateCommentContent(userId, commentId, newContent);

    // then
    then(userActivityRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("댓글 좋아요 취소 시 활동 내역 좋아요 목록에서 해당 항목을 제거합니다.")
  void shouldRemoveCommentLikeSummary() {
    // given
    UUID commentLikeId = UUID.randomUUID();

    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    CommentLikeSummary summary = new CommentLikeSummary(
        commentLikeId,
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

    document.addCommentLikeSummary(summary);

    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityService.removeCommentLikeSummary(userId, commentLikeId);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);

    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();

    assertNotNull(savedDocument);
    assertEquals(userId, savedDocument.getId());
    assertEquals(0, savedDocument.getCommentLikes().size());
  }

  @Test
  @DisplayName("댓글 좋아요 취소 시 문서가 없으면 정상 종료합니다.")
  void shouldDoNothingWhenUserActivityNotFoundOnRemoveCommentLikeSummary() {
    // given
    UUID commentLikeId = UUID.randomUUID();

    given(userActivityRepository.findById(userId)).willReturn(Optional.empty());

    // when
    userActivityService.removeCommentLikeSummary(userId, commentLikeId);

    // then
    then(userActivityRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("뉴스 기사 삭제 시 활동 내역에서 해당 기사 뷰를 삭제합니다.")
  void shouldRemoveArticleView_whenArticleRemoved() {
    //given
    UUID articleId = UUID.randomUUID();

    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    ArticleViewSummary articleViewSummary = new ArticleViewSummary(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.now(),
        articleId,
        "source1",
        "sourceUrl1",
        "title1",
        Instant.now(),
        "summary1",
        1,
        100
    );

    document.addArticleViewSummary(articleViewSummary);
    given(userActivityRepository.findAllByArticleViewsArticleId(articleId)).willReturn(List.of(document));

    // when
    userActivityService.removeArticleViewSummary(articleId);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);
    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();
    assertNotNull(savedDocument);
    assertEquals(userId, savedDocument.getId());
    assertEquals(0, savedDocument.getArticleViews().size());
  }

  @Test
  @DisplayName("구독 해제 시 활동 내역의 구독 목록에서 해당 구독을 삭제합니다.")
  void shouldRemoveSubscriptionSummary_whenSubscriptionCanceled() {
    //given
    UUID subscriptionId = UUID.randomUUID();

    UserActivityDocument document = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    SubscriptionSummary subscriptionSummary = new SubscriptionSummary(
        subscriptionId,
        UUID.randomUUID(),
        "interest1",
        List.of("keyword1"),
        1,
        Instant.now()
    );

    document.addSubscriptionSummary(subscriptionSummary);
    given(userActivityRepository.findById(userId)).willReturn(Optional.of(document));

    // when
    userActivityService.removeSubscriptionSummary(userId, subscriptionId);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);
    then(userActivityRepository).should().save(documentCaptor.capture());

    UserActivityDocument savedDocument = documentCaptor.getValue();
    assertNotNull(savedDocument);
    assertEquals(userId, savedDocument.getId());
    assertEquals(0, savedDocument.getSubscriptions().size());
  }

  @Test
  @DisplayName("관심사 키워드 업데이트 시 활동 내역에서 해당 관심사의 키워드를 업데이트합니다.")
  void shouldRemoveAllSubscriptionSummary_whenInterestDeleted() {
    // given
    UUID interestId = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();

    UserActivityDocument document1 = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    SubscriptionSummary subscriptionSummary1 = new SubscriptionSummary(
        UUID.randomUUID(),
        interestId,
        "interest1",
        List.of("keyword1"),
        1,
        Instant.now()
    );

    UserActivityDocument document2 = UserActivityDocument.create(
        userId2,
        "test2@test.com",
        "tester2",
        Instant.now()
    );

    SubscriptionSummary subscriptionSummary2 = new SubscriptionSummary(
        UUID.randomUUID(),
        interestId,
        "interest2",
        List.of("keyword2"),
        1,
        Instant.now()
    );

    document1.addSubscriptionSummary(subscriptionSummary1);
    document2.addSubscriptionSummary(subscriptionSummary2);
    given(userActivityRepository.findAllBySubscriptionsInterestId(interestId))
        .willReturn(List.of(document1, document2));

    // when
    userActivityService.removeAllSubscriptionSummaryByInterest(interestId);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);
    then(userActivityRepository).should(times(2)).save(documentCaptor.capture());

    List<UserActivityDocument> savedDocuments = documentCaptor.getAllValues();
    assertNotNull(savedDocuments);
    assertEquals(2, savedDocuments.size());
    assertTrue(savedDocuments.stream()
        .allMatch(doc -> doc.getSubscriptions().isEmpty()));
  }

  @Test
  @DisplayName("관심사 삭제 시 활동 내역에서 해당 관심사의 구독 정보를 삭제합니다.")
  void shouldUpdatedAllSubscriptionKeywordsSummary_whenInterestKeywordsUpdated() {
    // given
    UUID interestId = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    List<String> keywords = List.of("newKeyword1", "newKeyword2");

    UserActivityDocument document1 = UserActivityDocument.create(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    UserActivityDocument document2 = UserActivityDocument.create(
        userId2,
        "test2@test.com",
        "tester2",
        Instant.now()
    );

    SubscriptionSummary subscriptionSummary1 = new SubscriptionSummary(
        UUID.randomUUID(),
        interestId,
        "interest1",
        List.of("keyword1"),
        1,
        Instant.now()
    );


    document1.addSubscriptionSummary(subscriptionSummary1);
    document2.addSubscriptionSummary(subscriptionSummary1);
    given(userActivityRepository.findAllBySubscriptionsInterestId(interestId))
        .willReturn(List.of(document1, document2));

    // when
    userActivityService.updateSubscriptionsByKeywords(interestId, keywords);

    // then
    ArgumentCaptor<UserActivityDocument> documentCaptor =
        ArgumentCaptor.forClass(UserActivityDocument.class);
    then(userActivityRepository).should(times(2)).save(documentCaptor.capture());

    List<UserActivityDocument> savedDocuments = documentCaptor.getAllValues();
    assertNotNull(savedDocuments);
    assertEquals(2, savedDocuments.size());

    savedDocuments.forEach(savedDocument ->
        assertEquals(keywords, savedDocument.getSubscriptions().get(0).interestKeywords())
    );
  }
}

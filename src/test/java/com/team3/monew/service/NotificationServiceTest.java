package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import com.team3.monew.dto.notification.CommentLikedNotificationRequest;
import com.team3.monew.dto.notification.InterestNotificationRequest;
import com.team3.monew.dto.notification.NotificationDto;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.notification.NotificationConfirmForbiddenException;
import com.team3.monew.exception.notification.NotificationNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.mapper.NotificationMapper;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CommentRepository commentRepository;

  @Spy
  private NotificationMapper notificationMapper = Mappers.getMapper(NotificationMapper.class);

  @InjectMocks
  private NotificationService notificationService;

  private UUID userId1;
  private UUID userId2;
  private UUID userId3;
  private UUID actorUserId;
  private UUID resourceId;
  private User user1;
  private User user2;
  private User user3;
  private User actorUser;

  private UUID commentId1;
  private UUID interestId1;
  private UUID interestId2;

  private UUID likeNotiId1;
  private UUID likeNotiId2;
  private UUID interestNotiId1;
  private UUID interestNotiId2;
  private Notification likeNotification1;
  private Notification likeNotification2;
  private Notification interestNotification1;
  private Notification interestNotification2;

  @BeforeEach
  void setUp() {
    userId1 = UUID.randomUUID();
    userId2 = UUID.randomUUID();
    userId3 = UUID.randomUUID();
    actorUserId = UUID.randomUUID();
    resourceId = UUID.randomUUID();

    user1 = User.create("test1@test.com", "test1", "pwd");
    user2 = User.create("test2@test.com", "test2", "pwd");
    user3 = User.create("test3@test.com", "test3", "pwd");
    actorUser = User.create("actor@test.com", "actor", "pwd");
    ReflectionTestUtils.setField(user1, "id", userId1);
    ReflectionTestUtils.setField(user2, "id", userId2);
    ReflectionTestUtils.setField(user3, "id", userId3);
    ReflectionTestUtils.setField(actorUser, "id", actorUserId);

    commentId1 = UUID.randomUUID();
    interestId1 = UUID.randomUUID();
    interestId2 = UUID.randomUUID();

    likeNotiId1 = UUID.randomUUID();
    likeNotiId2 = UUID.randomUUID();
    likeNotification1 = Notification.create(user1, "test2님이 당신의 댓글을 좋아합니다.",
        NotificationResourceType.COMMENT, commentId1, user2);
    likeNotification2 = Notification.create(user1, "test3님이 당신의 댓글을 좋아합니다.",
        NotificationResourceType.COMMENT, commentId1, user3);
    ReflectionTestUtils.setField(likeNotification1, "id", likeNotiId1);
    ReflectionTestUtils.setField(likeNotification2, "id", likeNotiId2);
    ReflectionTestUtils.setField(likeNotification1, "createdAt",
        Instant.parse("2026-01-01T10:00:00Z"));
    ReflectionTestUtils.setField(likeNotification2, "createdAt",
        Instant.parse("2026-01-01T11:00:00Z"));

    interestNotiId1 = UUID.randomUUID();
    interestNotiId2 = UUID.randomUUID();
    interestNotification1 = Notification.create(user1, "interest1에 관한 기사가 10건 등록되었습니다.",
        NotificationResourceType.INTEREST,
        interestId1, null);
    interestNotification2 = Notification.create(user1, "interest2에 관한 기사가 3건 등록되었습니다.",
        NotificationResourceType.INTEREST,
        interestId2, null);
    ReflectionTestUtils.setField(interestNotification1, "id", interestNotiId1);
    ReflectionTestUtils.setField(interestNotification2, "id", interestNotiId2);
    ReflectionTestUtils.setField(interestNotification1, "createdAt",
        Instant.parse("2026-01-01T12:00:00Z"));//두개가 동일한 시간에 등록
    ReflectionTestUtils.setField(interestNotification2, "createdAt",
        Instant.parse("2026-01-01T12:00:00Z"));
  }

  @Test
  @DisplayName("댓글, 작성자, 사용자 아이디로 좋아요 알림 등록에 성공합니다.")
  void shouldRegisterLikeNotification() {
    // given
    UUID commentId = resourceId;
    NewsArticle article = Mockito.mock(NewsArticle.class);
    Comment comment = Comment.create(article, user1, "test");
    UUID writerId = userId1;
    CommentLikedNotificationRequest request = new CommentLikedNotificationRequest(actorUserId,
        commentId);

    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
    given(userRepository.findById(actorUserId)).willReturn(Optional.of(actorUser));

    // when
    notificationService.registerLikeNotification(request);

    // then
    ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
    then(notificationRepository).should().save(notificationCaptor.capture());
    Notification notification = notificationCaptor.getValue();

    assertNotNull(notification);
    assertEquals(commentId, notification.getResourceId());
    assertEquals(NotificationResourceType.COMMENT, notification.getResourceType());
    assertEquals(writerId, notification.getUser().getId());
    assertEquals(actorUserId, notification.getActorUser().getId());
    assertFalse(notification.isConfirmed());
    assertEquals(actorUser.getNickname() + "님이 나의 댓글을 좋아합니다.", notification.getContent());
  }

  @Test
  @DisplayName("좋아요를 누른 사용자를 찾을 수 없을 때 좋아요 알림 등록에 실패합니다.")
  void shouldThrowException_whenActorUserNotFound() {
    // given
    UUID commentId = resourceId;
    NewsArticle article = Mockito.mock(NewsArticle.class);
    Comment comment = Comment.create(article, user1, "test");
    CommentLikedNotificationRequest request = new CommentLikedNotificationRequest(actorUserId,
        commentId);

    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
    given(userRepository.findById(actorUserId)).willReturn(Optional.empty());

    // when & then
    assertThrows(UserNotFoundException.class,
        () -> notificationService.registerLikeNotification(request));
    then(notificationRepository).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("좋아요를 받은 댓글을 찾을 수 없을 때 좋아요 알림 등록에 실패합니다.")
  void shouldThrowException_whenCommentNotFound() {
    // given
    UUID commentId = resourceId;
    CommentLikedNotificationRequest request = new CommentLikedNotificationRequest(actorUserId,
        commentId);

    given(commentRepository.findById(commentId)).willReturn(Optional.empty());

    // when & then
    assertThrows(CommentNotFoundException.class,
        () -> notificationService.registerLikeNotification(request));
    then(notificationRepository).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("알림 요청(구독자 아이디, 관심사 아이디, 관심사 이름, 새로 등록된 뉴스 개수)목록으로 뉴스 알림 목록 등록에 성공합니다.")
  void shouldRegisterInterestNotifications() {
    // given
    UUID resourceId2 = UUID.randomUUID();
    InterestNotificationRequest request1 = new InterestNotificationRequest(resourceId, "test1", 5,
        List.of(userId1, actorUserId));
    InterestNotificationRequest request2 = new InterestNotificationRequest(resourceId2, "test2", 2,
        List.of(userId1));

    given(userRepository.getReferenceById(any(UUID.class))).willReturn(mock(User.class));
    // when
    notificationService.registerInterestNotification(List.of(request1, request2));
    // then
    ArgumentCaptor<List<Notification>> notificationCaptor = ArgumentCaptor.forClass(List.class);
    then(notificationRepository).should().saveAll(notificationCaptor.capture());
    List<Notification> notifications = notificationCaptor.getValue();

    assertNotNull(notifications);
    assertAll(
        () -> assertThat(notifications).hasSize(3),
        // 1. 리소스 타입 검증
        () -> assertThat(notifications).extracting(Notification::getResourceType)
            .containsOnly(NotificationResourceType.INTEREST),

        // 2. 메시지 내용 검증
        () -> assertThat(notifications).extracting(Notification::getContent)
            .anyMatch(content -> content.contains("test1") && content.contains("5"))
            .anyMatch(content -> content.contains("test2") && content.contains("2")),

        // 3. 리소스 아이디 매핑
        () -> assertThat(notifications).extracting(Notification::getResourceId)
            .contains(resourceId, resourceId2),

        // 4. 리소스 아이디별 알림 개수
        () -> assertThat(notifications).filteredOn(n -> n.getResourceId().equals(resourceId))
            .hasSize(2), // 구독자 2명
        () -> assertThat(notifications).filteredOn(n -> n.getResourceId().equals(resourceId2))
            .hasSize(1)  // 1명
    );
  }

  @Test
  @DisplayName("주어진 커서 페이지네이션을 적용하여 사용자 아이디로 확인하지 않은 알림 목록을 최신순으로 조회한다.(다음요소가 있는 경우)")
  void shouldFindAllNotConfirmedNotifications_whenUserIdAndAllPaginationConditionIsGiven() {
    //given
    UUID cursor = UUID.randomUUID();
    Instant after = Instant.now();
    Integer limit = 5;
    Long totalElement = 10L;

    Pageable pageable = PageRequest.of(0, limit, Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.asc("id")
    ));
    List<Notification> contents = List.of(
        interestNotification2, interestNotification1, likeNotification2, likeNotification1);
    Page<Notification> notifications = new PageImpl<>(contents, pageable, totalElement);

    given(notificationRepository.findAllNotConfirmedNotificationByUserId(eq(userId1), eq(cursor),
        eq(after), any(Pageable.class))).willReturn(notifications);
    given(notificationRepository.countByUserIdAndIsConfirmedFalse(eq(userId1))).willReturn(
        totalElement);

    //when
    CursorPageResponseDto<NotificationDto> result = notificationService.findAllNotConfirmed(userId1,
        cursor, after, limit);

    //then
    assertNotNull(result);
    then(notificationRepository).should()
        .countByUserIdAndIsConfirmedFalse(eq(userId1)); //커서 조건이 있기때문에 추가쿼리 필요
    assertAll(
        // 1. 컨텐츠 검증
        () -> assertThat(result.content()).hasSize(4),
        () -> assertThat(result.content().get(0).id()).isEqualTo(interestNotification2.getId()),

        // 2. 커서 검증
        () -> assertThat(result.nextCursor()).isEqualTo(likeNotification1.getId().toString()),
        () -> assertThat(result.nextAfter()).isEqualTo(likeNotification1.getCreatedAt()),

        // 3. 페이징 정보 검증
        () -> assertThat(result.totalElements()).isEqualTo(totalElement),
        () -> assertThat(result.hasNext()).isTrue(),
        () -> assertThat(result.size()).isEqualTo(limit)
    );
  }

  @Test
  @DisplayName("주어진 커서 페이지네이션을 적용하여 사용자 아이디로 확인하지 않은 알림 목록을 최신순으로 조회한다.(다음요소가 없는 경우)")
  void shouldNotHaveNext_whenLastPage() {
    //given
    UUID cursor = UUID.randomUUID();
    Instant after = Instant.now();
    Integer limit = 5;
    Long totalElement = 4L;

    Pageable pageable = PageRequest.of(0, limit, Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.asc("id")
    ));
    List<Notification> contents = List.of(likeNotification1, likeNotification2,
        interestNotification1, interestNotification2);

    Page<Notification> notifications = new PageImpl<>(contents, pageable, totalElement);

    given(notificationRepository.findAllNotConfirmedNotificationByUserId(eq(userId1), eq(cursor),
        eq(after), any(Pageable.class))).willReturn(notifications);
    given(notificationRepository.countByUserIdAndIsConfirmedFalse(eq(userId1))).willReturn(
        totalElement);

    //when
    CursorPageResponseDto<NotificationDto> result = notificationService.findAllNotConfirmed(userId1,
        cursor, after, limit);

    //then
    assertNotNull(result);
    then(notificationRepository).should()
        .countByUserIdAndIsConfirmedFalse(eq(userId1)); //커서 조건이 있기때문에 추가쿼리 필요
    assertAll(
        // 1. 컨텐츠 검증
        () -> assertThat(result.content()).hasSize(4),
        () -> assertThat(result.content().get(0).id()).isEqualTo(likeNotification1.getId()),

        // 2. 커서 검증
        () -> assertThat(result.nextCursor()).isNull(),
        () -> assertThat(result.nextAfter()).isNull(),

        // 3. 페이징 정보 검증
        () -> assertThat(result.totalElements()).isEqualTo(totalElement),
        () -> assertThat(result.hasNext()).isFalse(),
        () -> assertThat(result.size()).isEqualTo(limit)
    );
  }

  @Test
  @DisplayName("커서 조건이 없을 때(첫페이지) 사용자 아이디로 확인하지 않은 알림 목록을 최신순으로 조회한다.(다음요소가 있는 경우)")
  void shouldFindAllNotConfirmedNotifications_whenUserIdAndAnyPaginationConditionIsGiven() {
    //given
    UUID cursor = null;
    Instant after = null;
    Integer limit = 5;
    Long totalElement = 10L;

    Pageable pageable = PageRequest.of(0, limit, Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.asc("id")
    ));
    List<Notification> contents = List.of(likeNotification1, likeNotification2,
        interestNotification1, interestNotification2);

    Page<Notification> notifications = new PageImpl<>(contents, pageable, totalElement);

    given(notificationRepository.findAllNotConfirmedNotificationByUserId(eq(userId1), eq(cursor),
        eq(after), any(Pageable.class))).willReturn(notifications);

    //when
    CursorPageResponseDto<NotificationDto> result = notificationService.findAllNotConfirmed(userId1,
        cursor, after, limit);

    //then
    assertNotNull(result);
    //커서 조건이 있기때문에 추가쿼리 불필요
    then(notificationRepository)
        .should(never())
        .countByUserIdAndIsConfirmedFalse(any(UUID.class));
    assertAll(
        // 1. 컨텐츠 검증
        () -> assertThat(result.content()).hasSize(4),
        () -> assertThat(result.content().get(0).id()).isEqualTo(likeNotification1.getId()),

        // 2. 커서 검증
        () -> assertThat(result.nextCursor()).isEqualTo(interestNotification2.getId().toString()),
        () -> assertThat(result.nextAfter()).isEqualTo(interestNotification2.getCreatedAt()),

        // 3. 페이징 정보 검증
        () -> assertThat(result.totalElements()).isEqualTo(totalElement),
        () -> assertThat(result.hasNext()).isTrue(),
        () -> assertThat(result.size()).isEqualTo(limit)
    );
  }

  @Test
  @DisplayName("매퍼를 사용하여 알림 엔터티를 DTO 형태로 변환한다.")
  void shouldConvertNotificationEntityToDto() {
    //given
    Notification notification = Notification.create(user1, "ddd", NotificationResourceType.INTEREST,
        resourceId, null);
    ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(notification, "createdAt", Instant.parse("2026-01-01T10:00:00Z"));
    ReflectionTestUtils.setField(notification, "updatedAt", Instant.parse("2026-01-01T12:00:00Z"));
    //when
    NotificationDto dto = notificationMapper.toDto(notification);

    //then
    assertNotNull(dto);
    assertEquals(dto.id(), notification.getId());
    assertEquals(dto.resourceType(), notification.getResourceType());
    assertEquals(dto.content(), notification.getContent());
    assertEquals(dto.resourceId(), notification.getResourceId());
    assertEquals(dto.confirmed(), notification.isConfirmed());
    assertEquals(dto.createdAt(), notification.getCreatedAt());
    assertEquals(dto.updatedAt(), notification.getUpdatedAt());
  }

  @Test
  @DisplayName("사용자 아이디가 존재하지 않을 때 알림 확인 상태 변경에 실패한다.")
  void shouldFailToConfirmNotification_whenUserNotFound() {
    // given
    given(userRepository.existsById(userId1))
        .willReturn(false);

    // when & then
    assertThrows(UserNotFoundException.class, () -> {
      notificationService.confirm(userId1, likeNotification1.getId());
    });
  }

  @Test
  @DisplayName("알림이 존재하지 않을 때 알림 확인 상태 변경에 실패한다.")
  void shouldFailToConfirmNotification_whenNotificationNotFound() {
    // given
    given(userRepository.existsById(userId1)).willReturn(true);
    given(notificationRepository.findById(likeNotification1.getId()))
        .willReturn(Optional.empty());

    // when & then
    assertThrows(NotificationNotFoundException.class, () -> {
      notificationService.confirm(userId1, likeNotification1.getId());
    });
  }

  @Test
  @DisplayName("알림이 존재하지만 권한이 없는 사용자 일때, 알림 확인 상태 변경에 실패한다.")
  void shouldFailToConfirmNotification_whenUnAuthorized() {
    // given
    given(userRepository.existsById(userId2)).willReturn(true);
    given(notificationRepository.findById(likeNotification1.getId()))
        .willReturn(Optional.of(likeNotification1));

    // when & then
    assertThrows(NotificationConfirmForbiddenException.class, () -> {
      notificationService.confirm(userId2, likeNotification1.getId());
    });
  }

  @Test
  @DisplayName("사용자 아이디와 알림 아이디로 미확인 알림의 확인 상태 변경에 성공한다.")
  void shouldConfirmNotification() {
    // given
    given(userRepository.existsById(userId1)).willReturn(true);
    given(notificationRepository.findById(likeNotification1.getId()))
        .willReturn(Optional.of(likeNotification1));

    // when
    notificationService.confirm(userId1, likeNotification1.getId());

    assertTrue(likeNotification1.isConfirmed());
  }

  @Test
  @DisplayName("이미 확인된 알림은 확인 상태를 변경하지 않고 성공 응답을 보낸다.")
  void shouldConfirmNotificationWithNoCommit_WhenNotificationAlreadyConfirmed() {
    // given
    ReflectionTestUtils.setField(likeNotification1, "isConfirmed", true);
    ReflectionTestUtils.setField(likeNotification1, "confirmedAt",
        Instant.parse("2026-03-01T12:00:00Z"));

    given(userRepository.existsById(userId1)).willReturn(true);
    given(notificationRepository.findById(likeNotification1.getId()))
        .willReturn(Optional.of(likeNotification1));

    // when
    notificationService.confirm(userId1, likeNotification1.getId());

    assertTrue(likeNotification1.isConfirmed());
    assertEquals(likeNotification1.getConfirmedAt(),
        Instant.parse("2026-03-01T12:00:00Z"));//기존 확인 시간과 동일
  }

  @Test
  @DisplayName("사용자 아이디가 존재하지 않을 때 전체 알림 확인 상태 변경에 실패한다.")
  void shouldFailToConfirmAllNotifications_whenUserNotFound() {
    // given
    given(userRepository.existsById(userId1))
        .willReturn(false);

    // when & then
    assertThrows(UserNotFoundException.class, () -> {
      notificationService.confirmAll(userId1);
    });
  }

  @Test
  @DisplayName("유효한 사용자 아이디로 전체 미확인 알림의 확인 상태 변경에 성공한다.")
  void shouldConfirmAllNotifications() {
    // given
    given(userRepository.existsById(userId1)).willReturn(true);
    given(notificationRepository.confirmAllByUserId(eq(userId1), any(Instant.class)))
        .willReturn(4);//4개 알림 확인 상태로 변경

    // when
    notificationService.confirmAll(userId1);

    //then
    then(notificationRepository).should(times(1))
        .confirmAllByUserId(eq(userId1), any(Instant.class));
  }
}
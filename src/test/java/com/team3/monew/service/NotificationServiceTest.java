package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.team3.monew.dto.notification.CommentLikedNotificationRequest;
import com.team3.monew.dto.notification.InterestNotificationRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.UserRepository;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
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


  @InjectMocks
  private NotificationService notificationService;

  private UUID userId;
  private UUID actorUserId;
  private UUID resourceId;
  private User user;
  private User actorUser;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    actorUserId = UUID.randomUUID();
    resourceId = UUID.randomUUID();

    user = User.create("test@test.com", "test", "password");
    actorUser = User.create("actor@test.com", "actor", "password");
    ReflectionTestUtils.setField(user, "id", userId);
    ReflectionTestUtils.setField(actorUser, "id", actorUserId);
  }

  @Test
  @DisplayName("댓글, 작성자, 사용자 아이디로 좋아요 알림 등록에 성공합니다.")
  void shouldRegisterLikeNotification() {
    // given
    UUID commentId = resourceId;
    NewsArticle article = Mockito.mock(NewsArticle.class);
    Comment comment = Comment.create(article, user, "test");
    UUID writerId = userId;
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
    Comment comment = Comment.create(article, user, "test");
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
        List.of(userId, actorUserId));
    InterestNotificationRequest request2 = new InterestNotificationRequest(resourceId2, "test2", 2,
        List.of(userId));

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

}
package com.team3.monew.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team3.monew.dto.user.notification.CommentLikedNotificationRequest;
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
  @DisplayName("좋아요를 누른 사용자 찾을 수 없을 때 좋아요 알림 등록에 실패합니다.")
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
  }

  @Test
  @DisplayName("좋아요를 받은 댓글의 작성자를 찾을 수 없을 때 좋아요 알림 등록에 실패합니다.")
  void shouldThrowException_whenCommentWriterNotFound() {
    // given
    UUID commentId = resourceId;
    NewsArticle article = Mockito.mock(NewsArticle.class);
    Comment comment = Comment.create(article, null, "test");
    CommentLikedNotificationRequest request = new CommentLikedNotificationRequest(actorUserId,
        commentId);

    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    // when & then
    assertThrows(UserNotFoundException.class,
        () -> notificationService.registerLikeNotification(request));
  }
}
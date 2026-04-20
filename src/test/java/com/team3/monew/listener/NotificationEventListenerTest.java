package com.team3.monew.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.team3.monew.dto.user.notification.CommentLikedNotificationRequest;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.service.NotificationService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private NotificationEventListener notificationEventListener;

  @Test
  @DisplayName("리스너가 댓글 좋아요 이벤트를 받으면 서비스의 댓글 좋아요 알림 등록 메서드를 호출한다.")
  void shouldCallRegisterLikeNotificationMethod_whenListenCommentLikedEvent() {
    // given
    UUID actorUserId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    CommentLikedNotificationRequest request = new CommentLikedNotificationRequest(actorUserId,
        commentId);
    CommentLikedEvent event = new CommentLikedEvent(actorUserId, commentId);

    // when
    notificationEventListener.handleCommentLikedEvent(event);

    // then
    then(notificationService).should()
        .registerLikeNotification(eq(request));
  }

  @Test
  @DisplayName("알림 등록 중 커스텀 예외가 발생하면 리스너가 처리하고 예외를 외부로 전파하지 않는다.")
  void shouldCatchAndHandleBusinessException_whenServiceThrowsIt() {
    // given
    CommentLikedEvent event = new CommentLikedEvent(UUID.randomUUID(), UUID.randomUUID());

    // 서비스에서 댓글을 찾지 못함
    willThrow(new CommentNotFoundException(UUID.randomUUID()))
        .given(notificationService)
        .registerLikeNotification(any(CommentLikedNotificationRequest.class));

    // when & then
    assertDoesNotThrow(() -> notificationEventListener.handleCommentLikedEvent(event));
    then(notificationService).should().registerLikeNotification(any());
  }

  @Test
  @DisplayName("알림 등록 중 커스텀 예외 외의 예외가 발생하면 리스너는 외부로 전파한다.")
  void shouldThrowsException_whenServiceThrowsIt() {
    // given
    CommentLikedEvent event = new CommentLikedEvent(UUID.randomUUID(), UUID.randomUUID());
    willThrow(new RuntimeException("예상치 못한 예외"))
        .given(notificationService).registerLikeNotification(any());

    // when & then
    assertThrows(RuntimeException.class,
        () -> notificationEventListener.handleCommentLikedEvent(event));
  }
}
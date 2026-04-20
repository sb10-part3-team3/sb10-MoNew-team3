package com.team3.monew.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

import com.team3.monew.dto.user.notification.CommentLikedNotificationRequest;
import com.team3.monew.event.CommentLikedEvent;
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
}
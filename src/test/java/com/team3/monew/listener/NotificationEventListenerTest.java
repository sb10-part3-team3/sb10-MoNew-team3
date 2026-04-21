package com.team3.monew.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.team3.monew.dto.notification.CommentLikedNotificationRequest;
import com.team3.monew.dto.notification.InterestNotificationRequest;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.event.InterestNotificationEvent;
import com.team3.monew.event.InterestNotificationEvent.InterestArticleSummary;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.repository.SubscriptionRepository;
import com.team3.monew.repository.SubscriptionRepository.SubscriptionInfo;
import com.team3.monew.service.NotificationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @InjectMocks
  private NotificationEventListener notificationEventListener;

  // 레포지토리 인터페이스 객체 생성을 위한 구현체
  static class TestSubscriptionInfo implements SubscriptionInfo {

    private final UUID userId;
    private final UUID interestId;

    public TestSubscriptionInfo(UUID userId, UUID interestId) {
      this.userId = userId;
      this.interestId = interestId;
    }

    @Override
    public UUID getUserId() {
      return userId;
    }

    @Override
    public UUID getInterestId() {
      return interestId;
    }
  }

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
  @DisplayName("알림 등록 중 댓글 예외가 발생하면 리스너가 처리하고 예외를 외부로 전파하지 않는다.")
  void shouldCatchAndHandleCommentNotFoundsException_whenServiceThrowsIt() {
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
  @DisplayName("알림 등록 중 사용자 예외가 발생하면 리스너가 처리하고 예외를 외부로 전파하지 않는다.")
  void shouldCatchAndHandleUserNotFoundsException_whenServiceThrowsIt() {
    // given
    CommentLikedEvent event = new CommentLikedEvent(UUID.randomUUID(), UUID.randomUUID());

    // 서비스에서 댓글을 찾지 못함
    willThrow(new UserNotFoundException(UUID.randomUUID()))
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

  @Test
  @DisplayName("리스너가 관심사 뉴스 이벤트를 받으면 서비스의 관심사 뉴스 알림 등록 메서드를 호출한다.")
  void shouldCallRegisterInterestNotificationMethod_whenListenIt() {
    // given
    UUID interestId1 = UUID.randomUUID();
    UUID interestId2 = UUID.randomUUID();
    UUID interestId3 = UUID.randomUUID();
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    UUID userId3 = UUID.randomUUID();
    UUID userId4 = UUID.randomUUID();
    UUID userId5 = UUID.randomUUID();
    InterestArticleSummary summary1 = new InterestArticleSummary("interest1", 10);
    InterestArticleSummary summary2 = new InterestArticleSummary("interest2", 2);
    InterestArticleSummary summary3 = new InterestArticleSummary("interest3", 3);

    Map<UUID, InterestArticleSummary> map = new HashMap<>();
    map.put(interestId1, summary1);
    map.put(interestId2, summary2);
    map.put(interestId3, summary3);
    InterestNotificationEvent event = new InterestNotificationEvent(map);

    TestSubscriptionInfo subscriptionInfo1 = new TestSubscriptionInfo(userId1, interestId1);
    TestSubscriptionInfo subscriptionInfo2 = new TestSubscriptionInfo(userId2, interestId1);
    TestSubscriptionInfo subscriptionInfo3 = new TestSubscriptionInfo(userId3, interestId1);
    TestSubscriptionInfo subscriptionInfo4 = new TestSubscriptionInfo(userId4, interestId2);
    TestSubscriptionInfo subscriptionInfo5 = new TestSubscriptionInfo(userId5, interestId2);
    TestSubscriptionInfo subscriptionInfo6 = new TestSubscriptionInfo(userId1, interestId2);
    TestSubscriptionInfo subscriptionInfo7 = new TestSubscriptionInfo(userId2, interestId2);

    given(subscriptionRepository.findAllProjectedByInterestIdIn(
        Set.of(interestId1, interestId2, interestId3)))
        .willReturn(
            List.of(subscriptionInfo1, subscriptionInfo2, subscriptionInfo3, subscriptionInfo4,
                subscriptionInfo5, subscriptionInfo6, subscriptionInfo7));

    // when
    notificationEventListener.handleInterestNotificationEvent(event);

    // then
    then(notificationService).should(times(1))
        .registerInterestNotification(argThat(list ->
            list.size() == 2 && // 구독자가 있는 2개만
                list.stream().anyMatch(req -> req.interestName().equals("interest1")) &&
                list.stream().anyMatch(req -> req.interestName().equals("interest2"))
        ));
  }
}
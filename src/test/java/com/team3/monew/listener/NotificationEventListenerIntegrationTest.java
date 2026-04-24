package com.team3.monew.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;

import com.team3.monew.dto.notification.CommentLikedNotificationRequest;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.event.InterestNotificationEvent;
import com.team3.monew.event.InterestNotificationEvent.InterestArticleSummary;
import com.team3.monew.repository.SubscriptionRepository;
import com.team3.monew.service.NotificationService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
public class NotificationEventListenerIntegrationTest {

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @MockitoBean
  private NotificationService notificationService;

  @MockitoBean
  private SubscriptionRepository subscriptionRepository;

  @MockitoSpyBean
  private NotificationEventListener notificationEventListener;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Test
  @DisplayName("좋아요 이벤트를 발행하고 호출부에서 커밋이 이뤄지면 알림 리스너가 이를 수신한다.")
  void shouldCatchCommentLikedEvent_whenCommentLikedEventIsPublishedAndCommitted() {
    // given
    CommentLikedEvent event = CommentLikedEvent.of(UUID.randomUUID(), UUID.randomUUID());

    // when
    // 트랜잭션 종료 및 커밋
    transactionTemplate.execute(status -> {
      eventPublisher.publishEvent(event);
      return null;
    });

    // then
    // 비동기를 고려하여 타임 아웃 설정
    then(notificationEventListener).should(timeout(1000)).handleCommentLikedEvent(event);
  }

  @Test
  @DisplayName("좋아요 이벤트를 발행해도 호출부에서 커밋이 이뤄지지 않으면 알림 리스너가 이를 수신하지 않는다.")
  void shouldNotCatchCommentLikedEvent_whenCommentLikedEventIsPublishedButNotCommitted() {
    // given
    CommentLikedEvent event = CommentLikedEvent.of(UUID.randomUUID(), UUID.randomUUID());

    // when
    // 커밋 안됨
    eventPublisher.publishEvent(event);

    // then
    // 비동기 실행 시간을 고려
    then(notificationEventListener).should(after(1000).never()).handleCommentLikedEvent(event);
    then(notificationService).should(never())
        .registerLikeNotification(any(CommentLikedNotificationRequest.class));
  }

  @Test
  @DisplayName("좋아요 이벤트를 발행하고 호출부에서 커밋이 이뤄지면 알림 리스너가 이를 수신하여 서비스를 호출한다.")
  void shouldCatchCommentLikedEventAndCallRegisterLikeNotificationMethod_whenCommentLikedEventIsPublishedAndCommitted() {
    // given
    CommentLikedEvent event = CommentLikedEvent.of(UUID.randomUUID(), UUID.randomUUID());

    // when
    // 트랜잭션 종료 및 커밋
    transactionTemplate.execute(status -> {
      eventPublisher.publishEvent(event);
      return null;
    });

    // then
    //비동기 고려하여 타임 아웃 설정
    then(notificationEventListener).should(timeout(1000)).handleCommentLikedEvent(event);
    then(notificationService).should(timeout(1000)).registerLikeNotification(any(
        CommentLikedNotificationRequest.class));
  }

  @Test
  @DisplayName("좋아요 이벤트를 발행하면 알림 리스너는 메인 스레드와 다른 스레드에서 비동기로 실행된다.")
  void shouldHandleCommentLikedEventAsynchronously_whenCommentLikedEventIsPublished() {
    // given
    String mainThreadName = Thread.currentThread().getName();
    // 리스너가 실행된 스레드 이름 저장
    AtomicReference<String> listenerThreadName = new AtomicReference<>();

    // 서비스가 호출될 때, 실행 중인 스레드 이름 가로채서 저장
    willAnswer(invocation -> {
      listenerThreadName.set(Thread.currentThread().getName());
      return null;
    }).given(notificationService)
        .registerLikeNotification(any(CommentLikedNotificationRequest.class));

    CommentLikedEvent event = CommentLikedEvent.of(UUID.randomUUID(), UUID.randomUUID());

    // when
    transactionTemplate.execute(status -> {
      eventPublisher.publishEvent(event);
      return null;
    });

    // then
    // 스레드 값이 안 담기는 것 방지
    await()
        .atMost(1, java.util.concurrent.TimeUnit.SECONDS) // 최대 1초 대기
        .untilAsserted(() -> {
          then(notificationService).should()
              .registerLikeNotification(any(CommentLikedNotificationRequest.class));
          assertThat(listenerThreadName.get()).isNotNull();
          assertThat(listenerThreadName.get()).isNotEqualTo(mainThreadName);
          assertThat(listenerThreadName.get()).startsWith("real-time-noti-task-");//스레드풀 확인
        });
  }

  @Test
  @DisplayName("관심사 이벤트를 발행해도 호출부에서 커밋이 이뤄지지 않으면 알림 리스너가 이를 수신하지 않는다.")
  void shouldNotCatchInterestEvent_whenItIsNotCommitted() {
    // given
    Map<UUID, InterestArticleSummary> interestArticleSummaryMap = new HashMap<>();
    interestArticleSummaryMap.put(UUID.randomUUID(), new InterestArticleSummary("test1", 10));
    interestArticleSummaryMap.put(UUID.randomUUID(), new InterestArticleSummary("test2", 3));
    interestArticleSummaryMap.put(UUID.randomUUID(), new InterestArticleSummary("test3", 2));
    InterestNotificationEvent event = new InterestNotificationEvent(interestArticleSummaryMap);

    // when
    // 커밋 안됨
    eventPublisher.publishEvent(event);

    // then
    // 비동기 실행 시간을 고려
    then(notificationEventListener).should(after(1000).never())
        .handleInterestNotificationEvent(event);
    then(notificationService).should(never())
        .registerInterestNotification(anyList());
  }

  @Test
  @DisplayName("관심사 이벤트를 발행하고 호출부에서 커밋이 이뤄지면 알림 리스너가 이를 수신하여 내부코드를 실행한다.")
  void shouldCatchInterestEventAndDoInnerJob_whenItIsPublishedAndCommitted() {
    // given
    Map<UUID, InterestArticleSummary> interestArticleSummaryMap = new HashMap<>();
    interestArticleSummaryMap.put(UUID.randomUUID(), new InterestArticleSummary("test1", 10));
    interestArticleSummaryMap.put(UUID.randomUUID(), new InterestArticleSummary("test2", 3));
    interestArticleSummaryMap.put(UUID.randomUUID(), new InterestArticleSummary("test3", 2));
    InterestNotificationEvent event = new InterestNotificationEvent(interestArticleSummaryMap);

    // when
    // 트랜잭션 종료 및 커밋
    transactionTemplate.execute(status -> {
      eventPublisher.publishEvent(event);
      return null;
    });

    // then
    //비동기 고려하여 타임 아웃 설정
    then(notificationEventListener).should(timeout(1000)).handleInterestNotificationEvent(event);
    then(subscriptionRepository).should()
        .findAllProjectedByInterestIdIn(eq(interestArticleSummaryMap.keySet()));
  }

  @Test
  @DisplayName("관심사 이벤트를 발행하면 알림 리스너는 메인 스레드와 다른 스레드에서 비동기로 실행된다.")
  void shouldHandleInterestEventAsynchronously_whenItIsPublished() {
    // given
    String mainThreadName = Thread.currentThread().getName();
    // 리스너가 실행된 스레드 이름 저장
    AtomicReference<String> listenerThreadName = new AtomicReference<>();

    Map<UUID, InterestArticleSummary> interestArticleSummaryMap = new HashMap<>();
    interestArticleSummaryMap.put(UUID.randomUUID(), new InterestArticleSummary("test1", 10));
    interestArticleSummaryMap.put(UUID.randomUUID(), new InterestArticleSummary("test2", 3));
    interestArticleSummaryMap.put(UUID.randomUUID(), new InterestArticleSummary("test3", 2));
    InterestNotificationEvent event = new InterestNotificationEvent(interestArticleSummaryMap);

    // 서비스가 호출될 때, 실행 중인 스레드 이름 가로채서 저장
    willAnswer(invocation -> {
      listenerThreadName.set(Thread.currentThread().getName());
      return null;
    }).given(subscriptionRepository)
        .findAllProjectedByInterestIdIn(eq(interestArticleSummaryMap.keySet()));

    // when
    transactionTemplate.execute(status -> {
      eventPublisher.publishEvent(event);
      return null;
    });

    // then
    // 스레드 값이 안 담기는 것 방지
    await()
        .atMost(1, java.util.concurrent.TimeUnit.SECONDS) // 최대 1초 대기
        .untilAsserted(() -> {
          then(subscriptionRepository).should()
              .findAllProjectedByInterestIdIn(eq(interestArticleSummaryMap.keySet()));
          assertThat(listenerThreadName.get()).isNotNull();
          assertThat(listenerThreadName.get()).isNotEqualTo(mainThreadName);
          assertThat(listenerThreadName.get()).startsWith("batch-noti-task-");//스레드풀 확인
        });
  }
}

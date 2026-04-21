package com.team3.monew.listener;

import com.team3.monew.dto.notification.CommentLikedNotificationRequest;
import com.team3.monew.dto.notification.InterestNotificationRequest;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.event.InterestNotificationEvent;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.repository.SubscriptionRepository;
import com.team3.monew.repository.SubscriptionRepository.SubscriptionInfo;
import com.team3.monew.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

  private final NotificationService notificationService;
  private final SubscriptionRepository subscriptionRepository;

  @Async("realTimeNotificationTaskExecutor") //알림용 스레드풀 사용
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {
          org.springframework.dao.TransientDataAccessException.class,
          org.springframework.dao.CannotAcquireLockException.class}, // 일시적 DB 오류만 재시도
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)      // 1초 간격으로
  )
  public void handleCommentLikedEvent(CommentLikedEvent event) {
    try {
      notificationService.registerLikeNotification(
          new CommentLikedNotificationRequest(
              event.actorUserId(),
              event.commentId()
          )
      );
    } catch (CommentNotFoundException | UserNotFoundException e) {
      log.warn("알림 등록 취소: 관련 리소스를 찾을 수 없음: CommentId={}, ActorUserId={}, Message={}, Details={}",
          event.commentId(), event.actorUserId(), e.getMessage(), e.getDetails());
    } //그 외는 비동기 예외
  }

  @Async("batchNotificationTaskExecutor") //배치용 스레드풀 사용
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {
          org.springframework.dao.TransientDataAccessException.class,
          org.springframework.dao.CannotAcquireLockException.class}, // 일시적 DB 오류만 재시도
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)      // 1초 간격으로
  )
  public void handleInterestNotificationEvent(InterestNotificationEvent event) {
    try {
      Set<UUID> keySet = event.interestArticleSummaryMap().keySet();
      Map<UUID, List<SubscriptionInfo>> subscribers = subscriptionRepository.findAllProjectedByInterestIdIn(
              keySet).stream()
          .collect(Collectors.groupingBy(SubscriptionInfo::getInterestId));

      event.interestArticleSummaryMap().forEach((key, value) -> {
        List<UUID> ids = subscribers.getOrDefault(key, List.of()).stream()
            .map(SubscriptionInfo::getUserId).toList();
        if (ids.isEmpty()) {
          return;
        }
        notificationService.registerInterestNotification(
            new InterestNotificationRequest(
                key,
                value.interestName(),
                value.articleCount(),
                ids
            )
        );
      });

    } catch (BusinessException e) {
      log.warn("알림 등록 취소: 관련 리소스를 찾을 수 없음: Message={}, Details={}"
          , e.getMessage(), e.getDetails());
    } //그 외는 비동기 예외
  }
}

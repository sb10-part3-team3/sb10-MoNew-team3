package com.team3.monew.listener;

import com.team3.monew.dto.user.notification.CommentLikedNotificationRequest;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.service.NotificationService;
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
      log.info("알림 전송 취소: 관련 리소스를 찾을 수 없음: CommentId={}, ActorUserId={}, Message={}, Details={}",
          event.commentId(), event.actorUserId(), e.getMessage(), e.getDetails());
    } //그 외는 비동기 예외
  }
}

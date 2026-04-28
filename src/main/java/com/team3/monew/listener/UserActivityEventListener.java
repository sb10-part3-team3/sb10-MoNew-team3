package com.team3.monew.listener;

import com.team3.monew.event.ArticleViewEvent;
import com.team3.monew.event.CommentDeletedEvent;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.event.CommentRegisteredEvent;
import com.team3.monew.event.CommentUnlikedEvent;
import com.team3.monew.event.CommentUpdatedEvent;
import com.team3.monew.event.SubscriptionEvent;
import com.team3.monew.event.UserDeletedEvent;
import com.team3.monew.event.UserRegisteredEvent;
import com.team3.monew.event.UserUpdatedEvent;
import com.team3.monew.mapper.UserActivityMapper;
import com.team3.monew.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActivityEventListener {

  private final UserActivityService userActivityService;
  private final UserActivityMapper userActivityMapper;

  @Async("userActivityTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {
          TransientDataAccessException.class,
          CannotAcquireLockException.class
      },
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  public void handleUserRegisterEvent(UserRegisteredEvent event) {
      userActivityService.registerUserActivity(userActivityMapper.toRequest(event));
  }

  @Async("userActivityTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {
          TransientDataAccessException.class,
          CannotAcquireLockException.class
      },
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  public void handleSubscriptionEvent(SubscriptionEvent event) {
    userActivityService.updateSubscriptionSummary(event.userId(), userActivityMapper.toSubscriptionSummary(event));
  }

  @Async("userActivityTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {
          TransientDataAccessException.class,
          CannotAcquireLockException.class
      },
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  public void handleCommentRegisteredEvent(CommentRegisteredEvent event) {
    userActivityService.updateCommentSummary(userActivityMapper.toCommentSummary(event));
  }

  @Async("userActivityTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {
          TransientDataAccessException.class,
          CannotAcquireLockException.class
      },
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  public void handleCommentLikedEvent(CommentLikedEvent event) {
    userActivityService.updateCommentLikeSummary(event.actorUserId(), userActivityMapper.toCommentLikeSummary(event));
  }

  @Async("userActivityTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {
          TransientDataAccessException.class,
          CannotAcquireLockException.class
      },
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  public void handleArticleViewEvent(ArticleViewEvent event) {
    userActivityService.updateArticleViewSummary(event.userId(), userActivityMapper.toArticleViewSummary(event));
  }

  @Async("userActivityTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {
          TransientDataAccessException.class,
          CannotAcquireLockException.class
      },
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  public void handleUserUpdatedEvent(UserUpdatedEvent event) {
    userActivityService.updateUserNickname(event.userId(), event.nickname());
  }

  @Async("userActivityTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {
          TransientDataAccessException.class,
          CannotAcquireLockException.class
      },
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  public void handleUserDeletedEvent(UserDeletedEvent event) {
    userActivityService.deleteUserActivity(event.userId());
  }

  @Async("userActivityTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {TransientDataAccessException.class, CannotAcquireLockException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  public void handleCommentDeletedEvent(CommentDeletedEvent event) {
    userActivityService.removeCommentSummary(event.userId(), event.commentId());
  }

  @Async("userActivityTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {TransientDataAccessException.class, CannotAcquireLockException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  public void handleCommentUpdatedEvent(CommentUpdatedEvent event) {
    userActivityService.updateCommentContent(event.userId(), event.commentId(), event.content());
  }

  @Async("userActivityTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Retryable(
      retryFor = {TransientDataAccessException.class, CannotAcquireLockException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000)
  )
  public void handleCommentUnlikedEvent(CommentUnlikedEvent event) {
    userActivityService.removeCommentLikeSummary(event.actorUserId(), event.commentLikeId());
  }

  @Recover
  public void recover(Exception e, UserRegisteredEvent event) {
    log.error("사용자 활동 내역 UserRegistered Event 처리 실패: userId={}", event.userId(), e);
  }

  @Recover
  public void recover(Exception e, SubscriptionEvent event) {
    log.error("사용자 활동 내역 Subscription Event 처리 실패: userId={} subscriptionId={}", event.userId(), event.subscriptionId(), e);
  }

  @Recover
  public void recover(Exception e, CommentRegisteredEvent event) {
    log.error("사용자 활동 내역 CommentRegistered Event 처리 실패: userId={} commentId={}", event.userId(), event.commentId(), e);
  }

  @Recover
  public void recover(Exception e, CommentLikedEvent event) {
    log.error("사용자 활동 내역 CommentLiked Event 처리 실패: userId={} commentId={}", event.actorUserId(), event.commentId(), e);
  }

  @Recover
  public void recover(Exception e, ArticleViewEvent event) {
    log.error("사용자 활동 내역 ArticleView Event 처리 실패: userId={} articleId={}", event.userId(), event.articleId(), e);
  }

  @Recover
  public void recover(Exception e, UserUpdatedEvent event) {
    log.error("사용자 활동 내역 UserUpdated Event 처리 실패: userId={}", event.userId(), e);
  }

  @Recover
  public void recover(Exception e, CommentUpdatedEvent event) {
    log.error("사용자 활동 내역 CommentUpdated Event 처리 실패: userId={} commentId={}",
        event.userId(), event.commentId(), e);
  }
}

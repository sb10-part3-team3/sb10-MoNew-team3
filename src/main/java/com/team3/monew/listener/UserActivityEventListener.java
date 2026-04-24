package com.team3.monew.listener;

import com.team3.monew.event.ArticleViewEvent;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.event.CommentRegisteredEvent;
import com.team3.monew.event.SubscriptionEvent;
import com.team3.monew.event.UserRegisteredEvent;
import com.team3.monew.mapper.UserActivityMapper;
import com.team3.monew.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
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
}

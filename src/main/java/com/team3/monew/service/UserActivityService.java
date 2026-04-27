package com.team3.monew.service;

import com.team3.monew.document.ArticleViewSummary;
import com.team3.monew.document.CommentLikeSummary;
import com.team3.monew.document.CommentSummary;
import com.team3.monew.document.SubscriptionSummary;
import com.team3.monew.document.UserActivityDocument;
import com.team3.monew.document.UserActivityRequest;
import com.team3.monew.dto.useractivity.UserActivityDto;
import com.team3.monew.exception.useractivity.UserActivityConflictException;
import com.team3.monew.exception.useractivity.UserActivityNotFoundException;
import com.team3.monew.mapper.UserActivityMapper;
import com.team3.monew.repository.UserActivityRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

  private final UserActivityRepository userActivityRepository;
  private final UserActivityMapper userActivityMapper;

  public UserActivityDto findUserActivity(UUID userId) {
    log.debug("사용자 활동 내역 조회 시작: userId={}", userId);
    UserActivityDocument userActivityDocument = userActivityRepository.findById(userId)
        .orElseThrow(() -> new UserActivityNotFoundException(userId));
    log.info("사용자 활동 내역 조회 성공: userId={}", userId);
    return userActivityMapper.toDto(userActivityDocument);
  }

  public void registerUserActivity(UserActivityRequest userActivityRequest) {
    log.debug("사용자 활동 내역 등록 시작: userId={}", userActivityRequest.id());
    // 이벤트 처리 순서 상 getOrCreate로 이미 생성되었을 수도 있으니 체크 후 생성
    UserActivityDocument userActivityDocument = userActivityRepository.findById(userActivityRequest.id())
            .orElseGet(() -> userActivityMapper.toDocument(userActivityRequest));

    userActivityDocument.updateUserInfo(
        userActivityRequest.email(),
        userActivityRequest.nickname(),
        userActivityRequest.createdAt()
    );

    userActivityRepository.save(userActivityDocument);
    log.debug("사용자 활동 내역 등록 성공: userId={}", userActivityRequest.id());
  }

  @Retryable(
      retryFor = OptimisticLockingFailureException.class, // 이 예외일 때만 재시도
      maxAttempts = 3,                                    // 최초 1회 + 재시도 2회
      backoff = @Backoff(delay = 100)                     // 재시도 전 100ms 대기
  )
  public void updateSubscriptionSummary(UUID userId, SubscriptionSummary subscriptionSummary) {
    log.debug("사용자 활동 내역 구독 업데이트 시작: userId={} interestId={}", userId, subscriptionSummary.interestId());
    UserActivityDocument userActivityDocument = getOrCreate(userId);

    userActivityDocument.addSubscriptionSummary(subscriptionSummary);
    userActivityRepository.save(userActivityDocument);
    log.debug("사용자 활동 내역 구독 업데이트 성공: userId={} interestId={}", userId, subscriptionSummary.interestId());
  }

  @Retryable(
      retryFor = OptimisticLockingFailureException.class, // 이 예외일 때만 재시도
      maxAttempts = 3,                                    // 최초 1회 + 재시도 2회
      backoff = @Backoff(delay = 100)                     // 재시도 전 100ms 대기
  )
  public void updateCommentSummary(CommentSummary commentSummary) {
    log.debug("사용자 활동 내역 댓글 업데이트 시작: userId={} commentId={}", commentSummary.userId(), commentSummary.id());
    UserActivityDocument userActivityDocument = getOrCreate(commentSummary.userId());

    userActivityDocument.addCommentSummary(commentSummary);
    userActivityRepository.save(userActivityDocument);
    log.debug("사용자 활동 내역 댓글 업데이트 성공: userId={} commentId={}", commentSummary.userId(), commentSummary.id());
  }

  @Retryable(
      retryFor = OptimisticLockingFailureException.class, // 이 예외일 때만 재시도
      maxAttempts = 3,                                    // 최초 1회 + 재시도 2회
      backoff = @Backoff(delay = 100)                     // 재시도 전 100ms 대기
  )
  public void updateCommentLikeSummary(UUID userId, CommentLikeSummary commentLikeSummary) {
    log.debug("사용자 활동 내역 좋아요 업데이트 시작: userId={} commentLikeId={}", userId, commentLikeSummary.id());
    UserActivityDocument userActivityDocument = getOrCreate(userId);

    userActivityDocument.addCommentLikeSummary(commentLikeSummary);
    userActivityRepository.save(userActivityDocument);
    log.debug("사용자 활동 내역 좋아요 업데이트 성공: userId={} commentLikeId={}", userId, commentLikeSummary.id());
  }

  @Retryable(
      retryFor = OptimisticLockingFailureException.class, // 이 예외일 때만 재시도
      maxAttempts = 3,                                    // 최초 1회 + 재시도 2회
      backoff = @Backoff(delay = 100)                     // 재시도 전 100ms 대기
  )
  public void updateArticleViewSummary(UUID userId, ArticleViewSummary articleViewSummary) {
    log.debug("사용자 활동 내역 기사 뷰 업데이트 시작: userId={} articleViewId={}", userId, articleViewSummary.id());
    UserActivityDocument userActivityDocument = getOrCreate(userId);

    userActivityDocument.addArticleViewSummary(articleViewSummary);
    userActivityRepository.save(userActivityDocument);
    log.debug("사용자 활동 내역 기사 뷰 업데이트 성공: userId={} articleViewId={}", userId, articleViewSummary.id());
  }

  @Recover
  public void recoverUpdateSubscriptionSummary(
      OptimisticLockingFailureException e,
      UUID userId,
      SubscriptionSummary summary
  ) {
    // 3번 모두 실패했을 때 실행됨
    log.error("구독 요약 업데이트 최종 실패: userId={}, interestId={}",
        userId, summary.interestId(), e);
    throw new UserActivityConflictException(userId);
  }

  @Recover
  public void recoverUpdateCommentSummary(
      OptimisticLockingFailureException e,
      CommentSummary summary
  ) {
    log.error("댓글 요약 업데이트 최종 실패: userId={}, commentId={}",
        summary.userId(), summary.id(), e);
    throw new UserActivityConflictException(summary.userId());
  }

  @Recover
  public void recoverUpdateCommentLikeSummary(
      OptimisticLockingFailureException e,
      UUID userId,
      CommentLikeSummary summary
  ) {
    log.error("댓글 좋아요 요약 업데이트 최종 실패: userId={}, commentLikeId={}",
        userId, summary.id(), e);
    throw new UserActivityConflictException(userId);
  }

  @Recover
  public void recoverUpdateArticleViewSummary(
      OptimisticLockingFailureException e,
      UUID userId,
      ArticleViewSummary summary
  ) {
    log.error("기사 뷰 요약 업데이트 최종 실패: userId={}, articleViewId={}",
        userId, summary.id(), e);
    throw new UserActivityConflictException(userId);
  }

  private UserActivityDocument getOrCreate(UUID userId) {
    return userActivityRepository.findById(userId)
        .orElseGet(() -> UserActivityDocument.empty(userId));
  }
}

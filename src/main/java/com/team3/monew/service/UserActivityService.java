package com.team3.monew.service;

import com.team3.monew.document.ArticleViewSummary;
import com.team3.monew.document.CommentLikeSummary;
import com.team3.monew.document.CommentSummary;
import com.team3.monew.document.SubscriptionSummary;
import com.team3.monew.document.UserActivityDocument;
import com.team3.monew.document.UserActivityRequest;
import com.team3.monew.dto.useractivity.UserActivityDto;
import com.team3.monew.exception.useractivity.UserActivityConflictException;
import com.team3.monew.exception.useractivity.UserActivityException;
import com.team3.monew.exception.useractivity.UserActivityNotFoundException;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.mapper.UserActivityMapper;
import com.team3.monew.repository.UserActivityRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    userActivityRepository.incrementSubscriberCount(subscriptionSummary.interestId(), 1);
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

  @Retryable(
      retryFor = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 100)
  )
  public void updateUserNickname(UUID userId, String newNickname) {
    log.debug("사용자 활동 내역 닉네임 업데이트 시작: userId={}", userId);
    UserActivityDocument userActivityDocument = userActivityRepository.findById(userId)
        .orElseThrow(() -> new UserActivityNotFoundException(userId));
    userActivityDocument.updateNickname(newNickname);
    userActivityDocument.updateCommentNickname(newNickname);
    userActivityRepository.save(userActivityDocument);

    // 닉네임 변경하는 user가 작성한 모든 댓글 가져오고 닉네임 변경
    List<UserActivityDocument> documents = userActivityRepository.findAllByCommentsUserId(userId);
    documents.forEach(document -> {
      document.updateCommentNickname(newNickname);
      userActivityRepository.save(document);
    });

    log.debug("사용자 활동 내역 닉네임 업데이트 성공: userId={}", userId);
  }

  @Retryable(
      retryFor = {
          TransientDataAccessException.class,
          OptimisticLockingFailureException.class
      },
      maxAttempts = 3,
      backoff = @Backoff(delay = 100)
  )
  public void deleteUserActivity(UUID userId) {
    log.debug("사용자 활동 내역 삭제 시작: userId={}", userId);
    UserActivityDocument document = userActivityRepository.findById(userId)
        .orElse(null);
    if (document == null) {
      return;
    }
    // 다른 사람의 활동 내역 중 좋아요 한 댓글 삭제 반영
    List<UUID> commentIds = document.getComments().stream()
        .map(CommentSummary::id)
        .toList();

    userActivityRepository.findAllByCommentLikesCommentIdIn(commentIds)
        .forEach(otherDocument -> {
          commentIds.forEach(otherDocument::removeCommentLikeSummaryByCommentId);
          userActivityRepository.save(otherDocument);
        });
    userActivityRepository.deleteById(userId);
    log.debug("사용자 활동 내역 삭제 성공: userId={}", userId);
  }

  public void removeCommentSummary(UUID userId, UUID commentId) {
    log.debug("사용자 활동 내역 댓글 삭제 시작: userId={} commentId={}", userId, commentId);
    UserActivityDocument userActivityDocument = userActivityRepository.findById(userId)
        .orElse(null);
    if (userActivityDocument == null) {
      log.debug("사용자 활동 내역 문서가 이미 없어 댓글 삭제를 건너뜁니다: userId={} commentId={}", userId, commentId);
      return;
    }
    userActivityDocument.removeCommentSummary(commentId);
    userActivityRepository.save(userActivityDocument);

    // 삭제 처리하는 사용자가 쓴 댓글이 다른 사람의 좋아요 댓글에 있는 경우
    List<UserActivityDocument> documents = userActivityRepository.findAllByCommentLikesCommentId(commentId);
    documents.forEach(document -> {
      document.removeCommentLikeSummaryByCommentId(commentId);
      userActivityRepository.save(document);
    });
    log.debug("사용자 활동 내역 댓글 삭제 성공: userId={} commentId={}", userId, commentId);
  }

  @Retryable(
      retryFor = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 100)
  )
  public void updateCommentContent(UUID userId, UUID commentId, String newContent) {
    log.debug("사용자 활동 내역 댓글 수정 시작: userId={} commentId={}", userId, commentId);
    UserActivityDocument userActivityDocument = userActivityRepository.findById(userId)
        .orElse(null);
    if (userActivityDocument == null) {
      log.debug("사용자 활동 내역 문서가 이미 없어 댓글 수정를 건너뜁니다: userId={} commentId={}", userId, commentId);
      return;
    }
    userActivityDocument.updateCommentContent(commentId, newContent);
    userActivityRepository.save(userActivityDocument);

    // 다른 사람 활동내역 좋아요 댓글에도 수정 반영
    List<UserActivityDocument> documents = userActivityRepository.findAllByCommentLikesCommentId(commentId);
    documents.forEach(document -> {
      document.updateCommentLikeContent(commentId, newContent);
      userActivityRepository.save(document);
    });
    log.debug("사용자 활동 내역 댓글 수정 성공: userId={} commentId={}", userId, commentId);
  }

  public void removeCommentLikeSummary(UUID userId, UUID commentLikeId) {
    log.debug("사용자 활동 내역 댓글 좋아요 삭제 시작: userId={} commentLikeId={}", userId, commentLikeId);
    UserActivityDocument userActivityDocument = userActivityRepository.findById(userId)
        .orElse(null);
    if (userActivityDocument == null) {
      log.debug("사용자 활동 내역 문서가 이미 없어 좋아요 삭제를 건너뜁니다: userId={} commentLikeId={}", userId, commentLikeId);
      return;
    }
    userActivityDocument.removeCommentLikeSummary(commentLikeId);
    userActivityRepository.save(userActivityDocument);
    log.debug("사용자 활동 내역 댓글 좋아요 삭제 성공: userId={} commentLikeId={}", userId, commentLikeId);
  }

  public void removeArticleViewSummary(UUID articleId) {
    log.debug("사용자 활동 내역 기사 뷰 삭제 시작: articleId={}", articleId);
    List<UserActivityDocument> userActivityDocuments =
        userActivityRepository.findAllByArticleViewsArticleId(articleId);

    userActivityDocuments.forEach(userActivityDocument -> {
      userActivityDocument.removeArticleViewSummary(articleId);
      userActivityRepository.save(userActivityDocument);
    });
    log.debug("사용자 활동 내역 기사 뷰 삭제 성공: articleId={}", articleId);
  }

  public void removeSubscriptionSummary(UUID userId, UUID subscriptionId) {
    log.debug("사용자 활동 내역 구독 삭제 시작: userId={} subscriptionId={}", userId, subscriptionId);
    UserActivityDocument userActivityDocument = userActivityRepository.findById(userId)
        .orElse(null);
    if (userActivityDocument == null) {
      log.debug("사용자 활동 내역 문서가 이미 없어 구독 삭제를 건너뜁니다: userId={} subscriptionId={}", userId, subscriptionId);
      return;
    }
    userActivityDocument.getSubscriptions().stream()
        .filter(s -> Objects.equals(s.id(), subscriptionId))
        .map(SubscriptionSummary::interestId)
        .findFirst()
        .ifPresent(interestId -> {
          userActivityDocument.removeSubscriptionSummary(subscriptionId);
          userActivityRepository.save(userActivityDocument);
          // 다른 유저 문서의 구독자 수 감소
          userActivityRepository.incrementSubscriberCount(interestId, -1);
        });
    log.debug("사용자 활동 내역 구독 삭제 성공: userId={} subscriptionId={}", userId, subscriptionId);
  }

  public void removeAllSubscriptionSummaryByInterest(UUID interestId) {
    log.debug("사용자 활동 내역 관심사 삭제에 따른 구독 삭제 시작: interestId={}",interestId);
    List<UserActivityDocument> userActivityDocuments =
        userActivityRepository.findAllBySubscriptionsInterestId(interestId);
    userActivityDocuments.forEach(userActivityDocument -> {
      userActivityDocument.removeSubscriptionSummaryByInterestId(interestId);
      userActivityRepository.save(userActivityDocument);
    });
    log.debug("사용자 활동 내역 관심사 삭제에 따른 구독 삭제 성공: interestId={}",interestId);
  }

  @Retryable(
      retryFor = OptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 100)
  )
  public void updateSubscriptionsByKeywords(UUID interestId, List<String> keywords) {
    log.debug("사용자 활동 내역 관심사 키워드 업데이트 시작: interestId={}",interestId);
    List<UserActivityDocument> userActivityDocuments =
        userActivityRepository.findAllBySubscriptionsInterestId(interestId);
    userActivityDocuments.forEach(userActivityDocument -> {
      userActivityDocument.updateKeywords(interestId, keywords);
      userActivityRepository.save(userActivityDocument);
    });
    log.debug("사용자 활동 내역 관심사 키워드 업데이트 성공: interestId={}",interestId);
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

  @Recover
  public void recoverUpdateUserNickname(
      OptimisticLockingFailureException e,
      UUID userId,
      String newNickname
  ) {
    log.error("사용자 활동 내역 닉네임 업데이트 최종 실패: userId={}", userId, e);
    throw new UserActivityConflictException(userId);
  }

  private UserActivityDocument getOrCreate(UUID userId) {
    return userActivityRepository.findById(userId)
        .orElseGet(() -> UserActivityDocument.empty(userId));
  }

  @Recover
  public void recoverUpdateCommentContent(
      OptimisticLockingFailureException e,
      UUID userId,
      UUID commentId,
      String newContent
  ) {
    log.error("댓글 수정 활동 내역 업데이트 최종 실패: userId={}, commentId={}", userId, commentId, e);
    throw new UserActivityConflictException(userId);
  }

  @Recover
  public void recoverUpdateSubscriptionsByKeywords(
      OptimisticLockingFailureException e,
      UUID interestId,
      List<String> keywords
  ) {
    log.error("댓글 수정 활동 내역 업데이트 최종 실패: interestId={}, keywords={}", interestId, keywords, e);
    throw new UserActivityException(ErrorCode.USER_ACTIVITY_CONFLICT,
        Map.of("interestId", interestId, "keywords", keywords));
  }

  @Recover
  public void recoverDeleteUserActivity(Exception e, UUID userId) {
    log.error("사용자 활동 내역 삭제 최종 실패: userId={}", userId, e);
    throw new UserActivityConflictException(userId);
  }
}

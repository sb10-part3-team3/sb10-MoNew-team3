package com.team3.monew.service;

import com.team3.monew.document.ArticleViewSummary;
import com.team3.monew.document.CommentLikeSummary;
import com.team3.monew.document.CommentSummary;
import com.team3.monew.document.SubscriptionSummary;
import com.team3.monew.document.UserActivityDocument;
import com.team3.monew.document.UserActivityRequest;
import com.team3.monew.dto.useractivity.UserActivityDto;
import com.team3.monew.exception.useractivity.UserActivityNotFoundException;
import com.team3.monew.mapper.UserActivityMapper;
import com.team3.monew.repository.UserActivityRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityService {

  private final UserActivityRepository userActivityRepository;
  private final UserActivityMapper userActivityMapper;

  public UserActivityDto findUserActivity(UUID userId) {
    UserActivityDocument userActivityDocument = userActivityRepository.findById(userId)
        .orElseThrow(() -> new UserActivityNotFoundException(userId));
    return userActivityMapper.toDto(userActivityDocument);
  }

  public void registerUserActivity(UserActivityRequest userActivityRequest) {
    // 이벤트 처리 순서 상 getOrCreate로 이미 생성되었을 수도 있으니 체크 후 생성
    UserActivityDocument userActivityDocument = userActivityRepository.findById(userActivityRequest.id())
            .orElseGet(() -> userActivityMapper.toDocument(userActivityRequest));

    userActivityDocument.updateUserInfo(
        userActivityRequest.email(),
        userActivityRequest.nickname(),
        userActivityRequest.createdAt()
    );

    userActivityRepository.save(userActivityDocument);
  }

  public void updateSubscriptionSummary(UUID userId, SubscriptionSummary subscriptionSummary) {
    UserActivityDocument userActivityDocument = getOrCreate(userId);

    userActivityDocument.addSubscriptionSummary(subscriptionSummary);
    userActivityRepository.save(userActivityDocument);
  }

  public void updateCommentSummary(CommentSummary commentSummary) {
    UserActivityDocument userActivityDocument = getOrCreate(commentSummary.userId());

    userActivityDocument.addCommentSummary(commentSummary);
    userActivityRepository.save(userActivityDocument);
  }

  public void updateCommentLikeSummary(UUID userId, CommentLikeSummary commentLikeSummary) {
    UserActivityDocument userActivityDocument = getOrCreate(userId);

    userActivityDocument.addCommentLikeSummary(commentLikeSummary);
    userActivityRepository.save(userActivityDocument);
  }

  public void updateArticleViewSummary(UUID userId, ArticleViewSummary articleViewSummary) {
    UserActivityDocument userActivityDocument = getOrCreate(userId);

    userActivityDocument.addArticleViewSummary(articleViewSummary);
    userActivityRepository.save(userActivityDocument);
  }

  private UserActivityDocument getOrCreate(UUID userId) {
    return userActivityRepository.findById(userId)
        .orElseGet(() -> UserActivityDocument.empty(userId));
  }
}

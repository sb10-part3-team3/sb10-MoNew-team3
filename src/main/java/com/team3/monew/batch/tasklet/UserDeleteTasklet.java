package com.team3.monew.batch.tasklet;

import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.CommentLikeRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.SubscriptionRepository;
import com.team3.monew.repository.UserActivityRepository;
import com.team3.monew.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeleteTasklet {

  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final CommentRepository commentRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final UserActivityRepository userActivityRepository;
  private final ArticleViewRepository articleViewRepository;

  public RepeatStatus execute(Instant targetDate, int batchSize) {

    List<UUID> userIds = userRepository.findDeletableUserIds(
        targetDate,
        PageRequest.of(0, batchSize)
    );

    if (userIds.isEmpty()) {
      return RepeatStatus.FINISHED;
    }

    notificationRepository.deleteByUserIds(userIds);
    commentLikeRepository.deleteByUserIds(userIds);
    commentRepository.deleteByUserIds(userIds);
    subscriptionRepository.deleteByUserIds(userIds);
    articleViewRepository.deleteByUserIds(userIds);

    userActivityRepository.deleteByIdIn(userIds);
    userActivityRepository.removeEmbeddedCommentsByUserIds(userIds);

    int deleted = userRepository.deleteByIds(userIds);
    log.debug("deleted count={}", deleted);
    return deleted > 0 ? RepeatStatus.CONTINUABLE : RepeatStatus.FINISHED;
  }
}

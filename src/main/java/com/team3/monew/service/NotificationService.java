package com.team3.monew.service;

import com.team3.monew.dto.notification.CommentLikedNotificationRequest;
import com.team3.monew.dto.notification.CursorPageResponseNotificationDto;
import com.team3.monew.dto.notification.CursorPageResponseNotificationDto.NotificationDto;
import com.team3.monew.dto.notification.InterestNotificationRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.UserRepository;

import java.awt.print.Pageable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registerLikeNotification(CommentLikedNotificationRequest request) {
    log.debug("댓글 좋아요 알림 등록 시작: commentId={}, actorUserId={}", request.commentId(),
        request.actorUserId());
    Comment comment = commentRepository.findById(request.commentId())
        .orElseThrow(() -> new CommentNotFoundException(request.commentId()));
    log.debug("댓글 조회 성공: commentId={}", comment.getId());
    User user = comment.getUser();
    User actorUser = userRepository.findById(request.actorUserId())
        .orElseThrow(() -> new UserNotFoundException(request.actorUserId()));
    log.debug("좋아요 생성자 조회 성공: actorUserId={}", actorUser.getId());
    String content = generateCommentLikedContent(actorUser.getNickname());
    Notification notification = Notification.create(user, content, NotificationResourceType.COMMENT,
        request.commentId(), actorUser);
    log.debug("좋아요 알림 엔터티 생성 성공");
    notificationRepository.save(notification);
    log.info("댓글 좋아요 알림 등록 완료: notificationId={}", notification.getId());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registerInterestNotification(List<InterestNotificationRequest> requests) {
    log.debug("관심사 뉴스 알림 벌크 등록 시작: 요청 관심사 개수={}", requests.size());
    List<Notification> notifications = new ArrayList<>();
    requests.forEach(request -> {
      log.debug("관심사 뉴스 알림 생성 시작: interestId={}", request.interestId());
      request.subscriberIds().forEach(subscriberId -> {
        User user = userRepository.getReferenceById(subscriberId);
        String content = generateInterestNotificationContent(request.interestName(),
            request.articleCount());
        notifications.add(Notification.create(user, content, NotificationResourceType.INTEREST,
            request.interestId(), null));
      });
      log.debug("관심사 뉴스 알림 생성 완료: interestId={}", request.interestId());
    });
    notificationRepository.saveAll(notifications);
    log.info("관심사 뉴스 알림 벌크 등록 성공: 등록 개수={}", notifications.size());
  }

  @Transactional(readOnly = true)
  public CursorPageResponseNotificationDto findAllNotConfirmed(UUID requestUserId,
      Instant cursor, UUID after, Integer limit) {
    return null;
  }

  private String generateCommentLikedContent(String actorUserNickname) {
    return actorUserNickname + "님이 나의 댓글을 좋아합니다.";
  }

  private String generateInterestNotificationContent(String interestName, Integer articleCount) {
    return interestName + "와 관련된 기사가 " + articleCount + "건 등록되었습니다.";
  }
}

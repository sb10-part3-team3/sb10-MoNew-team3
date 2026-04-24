package com.team3.monew.service;

import com.team3.monew.dto.notification.CommentLikedNotificationRequest;
import com.team3.monew.dto.notification.InterestNotificationRequest;
import com.team3.monew.dto.notification.NotificationDto;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.notification.NotificationConfirmForbiddenException;
import com.team3.monew.exception.notification.NotificationNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.mapper.NotificationMapper;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
  private final NotificationMapper notificationMapper;

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
  public CursorPageResponseDto<NotificationDto> findAllNotConfirmed(UUID requestUserId,
      UUID cursor, Instant after, Integer limit) {
    log.debug("알림 목록 조회 시작: userId={}", requestUserId);
    Pageable pageable = PageRequest.of(0, limit, Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.asc("id")
    ));
    Page<NotificationDto> pages = notificationRepository.findAllNotConfirmedNotificationByUserId(
        requestUserId, cursor, after, pageable).map(notificationMapper::toDto);
    log.debug("알림 목록 데이터베이스 조회 성공: userId={}, cursor={}, after={}, limit{}", requestUserId, cursor,
        after, limit);
    List<NotificationDto> content = pages.getContent();
    boolean hasNext = pages.hasNext();
    UUID nextCursor = null;
    Instant nextAfter = null;

    if (hasNext && pages.hasContent()) {
      NotificationDto lastNotification = pages.getContent().get(content.size() - 1);
      nextCursor = lastNotification.id();
      nextAfter = lastNotification.createdAt();
    }

    Long totalElements = pages.getTotalElements();
    //전체 미확인 알림 개수 쿼리(첫페이지는 추가 쿼리X)
    if (after != null) {
      totalElements = notificationRepository.countByUserIdAndIsConfirmedFalse(requestUserId);
    }
    log.info("알림 목록 조회 완료: userId={}", requestUserId);
    return new CursorPageResponseDto<NotificationDto>(
        content,
        nextCursor == null ? null : nextCursor.toString(),
        nextAfter,
        pages.getSize(),
        totalElements,
        hasNext
    );
  }

  public void confirm(UUID requestUserId, UUID notificationId) {
    log.debug("개별 알림 확인 시작: userId={}, notificationId={}", requestUserId, notificationId);
    if (!userRepository.existsById(requestUserId)) {
      throw new UserNotFoundException(requestUserId);
    }
    log.debug("개별 알림 확인에서 사용자 유효성 확인 완료: userId={}, notificationId={}", requestUserId,
        notificationId);
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new NotificationNotFoundException(notificationId));
    log.debug("개별 알림 확인에서 알림 조회 성공: notificationId={}", notificationId);
    if (!notification.getUser().getId().equals(requestUserId)) {
      throw new NotificationConfirmForbiddenException(notificationId, requestUserId);
    }
    notification.confirm();
    log.info("개별 알림 확인 요청 성공: userId={}, notificationId={}", requestUserId, notificationId);
  }

  private String generateCommentLikedContent(String actorUserNickname) {
    return actorUserNickname + "님이 나의 댓글을 좋아합니다.";
  }

  private String generateInterestNotificationContent(String interestName, Integer articleCount) {
    return interestName + "와 관련된 기사가 " + articleCount + "건 등록되었습니다.";
  }
}

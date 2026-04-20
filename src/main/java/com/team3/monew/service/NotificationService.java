package com.team3.monew.service;

import com.team3.monew.dto.user.notification.CommentLikedNotificationRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;

  public void registerLikeNotification(CommentLikedNotificationRequest request) {
    log.debug("댓글 좋아요 알림 등록 시작: commentId={}, actorUserId={}", request.commentId(),
        request.actorUserId());
    Comment comment = commentRepository.findById(request.commentId())
        .orElseThrow(() -> new CommentNotFoundException());
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

  private String generateCommentLikedContent(String actorUserNickname) {
    return actorUserNickname + "님이 나의 댓글을 좋아합니다.";
  }
}

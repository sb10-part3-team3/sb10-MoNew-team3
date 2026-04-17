package com.team3.monew.service;

import com.team3.monew.dto.user.notification.CommentLikedNotificationRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

  public void registerLikeNotification(CommentLikedNotificationRequest request) {

  }
}

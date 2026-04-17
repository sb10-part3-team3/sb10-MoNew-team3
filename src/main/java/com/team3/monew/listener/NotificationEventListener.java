package com.team3.monew.listener;

import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentLikedEvent(CommentLikedEvent event) {
    notificationService.registerLikeNotification(
        event.actorUserId(),
        event.commentId(),
        event.writerId()
    );
  }
}

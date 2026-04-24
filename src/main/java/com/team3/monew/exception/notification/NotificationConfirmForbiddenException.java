package com.team3.monew.exception.notification;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class NotificationConfirmForbiddenException extends NotificationException {

  public NotificationConfirmForbiddenException(UUID notificationId, UUID requestUserId) {
    super(ErrorCode.NOTIFICATION_CONFIRM_FORBIDDEN,
        Map.of("notificationId", notificationId, "userId", requestUserId));
  }
}

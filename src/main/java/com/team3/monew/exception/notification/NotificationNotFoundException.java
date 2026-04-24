package com.team3.monew.exception.notification;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class NotificationNotFoundException extends NotificationException {

  public NotificationNotFoundException(UUID notificationId) {
    super(ErrorCode.NOTIFICATION_NOT_FOUND, Map.of("notificationId", notificationId));
  }
}

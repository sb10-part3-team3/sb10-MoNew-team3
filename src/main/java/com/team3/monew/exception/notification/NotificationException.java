package com.team3.monew.exception.notification;

import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import java.util.Map;

public class NotificationException extends BusinessException {

  public NotificationException(ErrorCode errorCode) {
    super(errorCode, Map.of());
  }

  public NotificationException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}

package com.team3.monew.exception.useractivity;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class UserActivityNotFoundException extends UserActivityException{

  public UserActivityNotFoundException(UUID userId) {
    super(ErrorCode.USER_ACTIVITY_NOT_FOUND, Map.of("userId", userId));
  }
}

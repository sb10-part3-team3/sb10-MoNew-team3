package com.team3.monew.exception.useractivity;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class UserActivityConflictException extends UserActivityException{

  public UserActivityConflictException(UUID userId) {
    super(ErrorCode.USER_ACTIVITY_CONFLICT, Map.of("userId", userId));
  }
}

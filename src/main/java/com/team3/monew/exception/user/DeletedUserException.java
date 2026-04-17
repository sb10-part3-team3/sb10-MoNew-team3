package com.team3.monew.exception.user;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class DeletedUserException extends UserException {

  public DeletedUserException(UUID userId) {
    super(ErrorCode.USER_DELETED, Map.of("userId", userId));
  }
}

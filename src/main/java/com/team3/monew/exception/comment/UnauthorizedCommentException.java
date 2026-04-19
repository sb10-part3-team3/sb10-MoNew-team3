package com.team3.monew.exception.comment;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class UnauthorizedCommentException extends CommentException {

  public UnauthorizedCommentException(UUID commentId) {
    super(ErrorCode.COMMENT_UPDATE_FORBIDDEN, Map.of("commentId", commentId));
  }
}

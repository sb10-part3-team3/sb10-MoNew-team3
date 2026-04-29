package com.team3.monew.exception.comment;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class UnauthorizedCommentUpdateException extends CommentException {

  public UnauthorizedCommentUpdateException(UUID commentId) {
    super(ErrorCode.COMMENT_UPDATE_FORBIDDEN, Map.of("commentId", commentId));
  }
}

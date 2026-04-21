package com.team3.monew.exception.comment;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class UnauthorizedCommentDeleteException extends CommentException {

  public UnauthorizedCommentDeleteException(UUID commentId) {
    super(ErrorCode.COMMENT_DELETE_FORBIDDEN, Map.of("commentId", commentId));
  }
}

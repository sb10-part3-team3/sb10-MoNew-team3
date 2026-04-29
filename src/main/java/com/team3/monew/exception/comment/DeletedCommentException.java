package com.team3.monew.exception.comment;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class DeletedCommentException extends CommentException {

  public DeletedCommentException(UUID commentId) {
    super(ErrorCode.COMMENT_DELETED, Map.of("commentId", commentId));
  }
}

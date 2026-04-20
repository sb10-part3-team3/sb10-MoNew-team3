package com.team3.monew.exception.comment;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class CommentNotFoundException extends CommentException {

  public CommentNotFoundException(UUID commentId) {
    super(ErrorCode.COMMENT_NOT_FOUND, Map.of("commentId", commentId));
  }
}

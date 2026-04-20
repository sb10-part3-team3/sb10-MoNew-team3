package com.team3.monew.exception.comment;

import com.team3.monew.global.enums.ErrorCode;

public class CommentNotFoundException extends CommentException {

  public CommentNotFoundException() {
    super(ErrorCode.COMMENT_NOT_FOUND);
  }
}

package com.team3.monew.exception.comment;

import com.team3.monew.global.enums.ErrorCode;
import java.util.Map;

public class CommentLikeNotFoundException extends CommentException {

  public CommentLikeNotFoundException() {
    super(ErrorCode.INVALID_INPUT_VALUE, Map.of("commentLike", "notFound"));
  }
}

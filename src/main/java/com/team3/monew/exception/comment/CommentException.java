package com.team3.monew.exception.comment;

import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import java.util.Map;

public class CommentException extends BusinessException {

  public CommentException(ErrorCode errorCode) {
    super(errorCode);
  }

  public CommentException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}

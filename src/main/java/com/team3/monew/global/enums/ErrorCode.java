package com.team3.monew.global.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임은 비어있지 않고 2자 이상, 10자 이하이어야 합니다."),
  INVALID_CREDENTIAL(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
  INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "올바르지 않은 비밀번호 형식입니다."),
  EMAIL_DUPLICATION(HttpStatus.CONFLICT, "해당 이메일이 이미 존재합니다."),
  ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "기사를 찾을 수 없습니다."),
  ARTICLE_DELETED(HttpStatus.BAD_REQUEST, "삭제된 기사입니다."),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
  COMMENT_DELETED(HttpStatus.NOT_FOUND, "삭제된 댓글입니다."),
  COMMENT_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "댓글 수정 권한이 없습니다."),
  COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "댓글 삭제 권한이 없습니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  USER_DELETED(HttpStatus.BAD_REQUEST, "삭제된 사용자입니다."),

  // INTEREST EXCEPTION
  INTEREST_NAME_DUPLICATE(HttpStatus.CONFLICT, "중복된 관심사 이름입니다."),
  INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 관심사를 찾을 수 없습니다."),
  INTEREST_KEYWORD_LIST_IS_BLANK(HttpStatus.BAD_REQUEST, "관심사 키워드가 비어있습니다."),
  INTEREST_KEYWORD_DUPLICATED(HttpStatus.BAD_REQUEST, "중복된 관심사 키워드가 존재합니다."),
  INTEREST_ALREADY_SUBSCRIBING(HttpStatus.CONFLICT, "이미 구독중인 관심사입니다."),
  INTEREST_NOT_SUBSCRIBING(HttpStatus.BAD_REQUEST, "구독하지 않은 관심사입니다."),

  // NOTIFICATION
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
  NOTIFICATION_CONFIRM_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 알림에 대한 확인 권한이 없습니다."),
  //COMMON
  INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "해당 파라미터가 유효하지 않습니다."),
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 형식이 올바르지 않습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

  ;

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }
}

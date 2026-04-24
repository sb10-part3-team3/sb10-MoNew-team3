package com.team3.monew.entity.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationResourceType {
  INTEREST,
  COMMENT;

  @JsonValue // JSON 직렬할때 소문자로
  public String toValue() {
    return this.name().toLowerCase();
  }
}

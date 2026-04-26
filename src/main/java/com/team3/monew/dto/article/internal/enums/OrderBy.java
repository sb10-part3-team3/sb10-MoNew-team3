package com.team3.monew.dto.article.internal.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum OrderBy {
  PUBLISH_DATE("publishDate"),
  COMMENT_COUNT("commentCount"),
  VIEW_COUNT("viewCount");

  private final String value;

  @JsonValue
  public String getValue() {    // Swagger문서 변환용
    return value;
  }

  public static OrderBy fromValue(String value) {
    for (OrderBy orderBy : OrderBy.values()) {
      if (orderBy.value.equalsIgnoreCase(value)) {
        return orderBy;
      }
    }

    return null;
  }
}

package com.team3.monew.dto.article.internal.enums;

public enum ArticleDirection {
  ASC,
  DESC;

  public static ArticleDirection fromValue(String value) {
    for (ArticleDirection direction : ArticleDirection.values()) {
      if (direction.name().equalsIgnoreCase(value)) {
        return direction;
      }
    }

    return null;
  }
}

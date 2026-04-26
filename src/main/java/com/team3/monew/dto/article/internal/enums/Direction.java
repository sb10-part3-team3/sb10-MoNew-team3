package com.team3.monew.dto.article.internal.enums;

public enum Direction {
  ASC,
  DESC;

  public static Direction fromValue(String value) {
    for (Direction direction : Direction.values()) {
      if (direction.name().equalsIgnoreCase(value)) {
        return direction;
      }
    }

    return null;
  }
}

package com.team3.monew.repository;

import com.team3.monew.entity.Comment;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  String ORDER_BY_CREATED_AT = "createdAt";
  String ORDER_BY_LIKE_COUNT = "likeCount";
  String CURSOR_DELIMITER = "\\|";

  default List<Comment> findActiveComments(
      UUID articleId,
      String orderBy,
      String cursor,
      Instant after,
      int limit
  ) {
    if (limit < 1) {
      throwInvalidInput("limit", limit);
    }

    Pageable pageable = PageRequest.of(0, limit);

    if (ORDER_BY_CREATED_AT.equals(orderBy)) {
      String[] cursorValues = splitCursor(cursor);
      Instant cursorCreatedAt = parseInstant(cursorValue(cursorValues, 0), "cursor");
      return findActiveCommentsByCreatedAtDesc(articleId, cursorCreatedAt, pageable);
    }

    if (ORDER_BY_LIKE_COUNT.equals(orderBy)) {
      String[] cursorValues = splitCursor(cursor);
      Integer cursorLikeCount = parseInteger(cursorValue(cursorValues, 0), "cursor");
      Instant cursorCreatedAt = parseInstant(cursorValue(cursorValues, 1), "cursorCreatedAt");

      if (cursorCreatedAt == null) {
        cursorCreatedAt = after;
      }
      return findActiveCommentsByLikeCountDesc(
          articleId,
          cursorLikeCount,
          cursorCreatedAt,
          pageable
      );
    }

    throwInvalidInput("orderBy", orderBy);
    return List.of();
  }

  @Query("""
      select c from Comment c join fetch c.article join fetch c.user
      where c.deleteStatus = com.team3.monew.entity.enums.DeleteStatus.ACTIVE and c.article.id = :articleId
        and (:cursorCreatedAt is null or c.createdAt < :cursorCreatedAt)
      order by c.createdAt desc
      """)
  List<Comment> findActiveCommentsByCreatedAtDesc(
      @Param("articleId") UUID articleId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable
  );

  @Query("""
      select c from Comment c join fetch c.article join fetch c.user
      where c.deleteStatus = com.team3.monew.entity.enums.DeleteStatus.ACTIVE and c.article.id = :articleId
        and (:cursorLikeCount is null or c.likeCount < :cursorLikeCount
          or (:cursorCreatedAt is not null and c.likeCount = :cursorLikeCount and c.createdAt < :cursorCreatedAt))
      order by c.likeCount desc, c.createdAt desc
      """)
  List<Comment> findActiveCommentsByLikeCountDesc(
      @Param("articleId") UUID articleId,
      @Param("cursorLikeCount") Integer cursorLikeCount,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      Pageable pageable
  );

  @Query("""
      select count(c) from Comment c
      where c.deleteStatus = com.team3.monew.entity.enums.DeleteStatus.ACTIVE and c.article.id = :articleId
      """)
  long countActiveComments(@Param("articleId") UUID articleId);

  private String[] splitCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return new String[0];
    }
    return cursor.split(CURSOR_DELIMITER);
  }

  private String cursorValue(String[] cursorValues, int index) {
    if (cursorValues.length <= index) {
      return null;
    }
    return cursorValues[index];
  }

  private Instant parseInstant(String value, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException e) {
      throwInvalidInput(field, value);
      return null;
    }
  }

  private Integer parseInteger(String value, String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throwInvalidInput(field, value);
      return null;
    }
  }

  private void throwInvalidInput(String field, Object value) {
    throw new BusinessException(
        ErrorCode.INVALID_INPUT_VALUE,
        Map.of(field, String.valueOf(value))
    );
  }
}

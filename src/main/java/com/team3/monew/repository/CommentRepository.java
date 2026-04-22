package com.team3.monew.repository;

import com.team3.monew.entity.Comment;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  default List<Comment> findActiveComments(
      UUID articleId,
      String orderBy,
      String direction,
      String cursor,
      Instant after,
      int limit
  ) {
    throw new UnsupportedOperationException("댓글 목록 조회 쿼리는 아직 구현되지 않았습니다.");
  }

  default long countActiveComments(UUID articleId) {
    throw new UnsupportedOperationException("댓글 개수 조회 쿼리는 아직 구현되지 않았습니다.");
  }
}

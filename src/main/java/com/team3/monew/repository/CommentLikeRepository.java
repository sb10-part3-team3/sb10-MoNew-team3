package com.team3.monew.repository;

import com.team3.monew.entity.CommentLike;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

  void deleteByCommentId(UUID commentId);

  default Set<UUID> findLikedCommentIds(UUID userId, List<UUID> commentIds) {
    throw new UnsupportedOperationException("댓글 좋아요 여부 조회 쿼리는 아직 구현되지 않았습니다.");
  }
}

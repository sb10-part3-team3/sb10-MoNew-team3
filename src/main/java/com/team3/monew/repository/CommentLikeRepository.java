package com.team3.monew.repository;

import com.team3.monew.entity.CommentLike;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

  void deleteByCommentId(UUID commentId);

  boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

  Optional<CommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);

  @Query("""
      select commentLike.comment.id
      from CommentLike commentLike
      where commentLike.user.id = :userId
        and commentLike.comment.id in :commentIds
      """)
  Set<UUID> findLikedCommentIds(
      @Param("userId") UUID userId,
      @Param("commentIds") List<UUID> commentIds
  );

  void deleteAllByUserId(UUID userId);
}

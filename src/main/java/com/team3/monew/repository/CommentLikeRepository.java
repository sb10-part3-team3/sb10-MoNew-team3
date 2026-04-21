package com.team3.monew.repository;

import com.team3.monew.entity.CommentLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

  void deleteByCommentId(UUID commentId);
}

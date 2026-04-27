package com.team3.monew.repository;

import com.team3.monew.entity.Comment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

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

  @Query("""
      select c
      from Comment c
      join fetch c.article
      join fetch c.user
      where c.id = :id
      """)
  Optional<Comment> findByIdWithArticleAndUser(@Param("id") UUID id);
}

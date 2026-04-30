package com.team3.monew.repository;

import com.team3.monew.entity.Comment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  @Query("""
      select c from Comment c join fetch c.article join fetch c.user
      where c.deleteStatus = com.team3.monew.entity.enums.DeleteStatus.ACTIVE and c.article.id = :articleId
      order by c.createdAt desc, c.id desc
      """)
  List<Comment> findFirstActiveCommentsByCreatedAtDesc(
      @Param("articleId") UUID articleId,
      Pageable pageable
  );

  @Query("""
      select c from Comment c join fetch c.article join fetch c.user
      where c.deleteStatus = com.team3.monew.entity.enums.DeleteStatus.ACTIVE and c.article.id = :articleId
        and (
          c.createdAt < :cursorCreatedAt
          or (c.createdAt = :cursorCreatedAt and c.id < :cursorId)
        )
      order by c.createdAt desc, c.id desc
      """)
  List<Comment> findActiveCommentsByCreatedAtDesc(
      @Param("articleId") UUID articleId,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      @Param("cursorId") UUID cursorId,
      Pageable pageable
  );

  @Query("""
      select c from Comment c join fetch c.article join fetch c.user
      where c.deleteStatus = com.team3.monew.entity.enums.DeleteStatus.ACTIVE and c.article.id = :articleId
      order by c.likeCount desc, c.createdAt desc, c.id desc
      """)
  List<Comment> findFirstActiveCommentsByLikeCountDesc(
      @Param("articleId") UUID articleId,
      Pageable pageable
  );

  @Query("""
      select c from Comment c join fetch c.article join fetch c.user
      where c.deleteStatus = com.team3.monew.entity.enums.DeleteStatus.ACTIVE and c.article.id = :articleId
        and (
          c.likeCount < :cursorLikeCount
          or (
            c.likeCount = :cursorLikeCount
            and (
              c.createdAt < :cursorCreatedAt
              or (c.createdAt = :cursorCreatedAt and c.id < :cursorId)
            )
          )
        )
      order by c.likeCount desc, c.createdAt desc, c.id desc
      """)
  List<Comment> findActiveCommentsByLikeCountDesc(
      @Param("articleId") UUID articleId,
      @Param("cursorLikeCount") Integer cursorLikeCount,
      @Param("cursorCreatedAt") Instant cursorCreatedAt,
      @Param("cursorId") UUID cursorId,
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

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM Comment c WHERE c.article.id = :articleId")
  void deleteAllByArticleId(@Param("articleId") UUID articleId);

  List<Comment> findAllByArticleId(UUID articleId);

  void deleteAllByUserId(UUID userId);

  @Modifying
  @Query("DELETE FROM Comment c WHERE c.user.id IN :userIds")
  void deleteByUserIds(List<UUID> userIds);
}

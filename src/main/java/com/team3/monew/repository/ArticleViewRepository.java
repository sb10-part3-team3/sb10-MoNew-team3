package com.team3.monew.repository;

import com.team3.monew.entity.ArticleView;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  @Query("SELECT av.article.id FROM ArticleView av WHERE av.article.id IN :articleIds AND av.user.id = :userId")
  Set<UUID> findAllByArticleIdInAndUserId(
      @Param("articleIds") Collection<UUID> articleIds,
      @Param("userId") UUID userId);

  Optional<ArticleView> findByArticleIdAndUserId(UUID articleId, UUID userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM ArticleView av WHERE av.article.id = :articleId")
  void deleteAllByArticleId(@Param("articleId") UUID articleId);

  List<ArticleView> findAllByArticleId(@Param("articleId") UUID articleId);

  void deleteAllByUserId(@Param("userId") UUID userId);

  @Modifying
  @Query("DELETE FROM ArticleView av WHERE av.user.id IN :userIds")
  void deleteByUserIds(@Param("userIds") List<UUID> userIds);
}

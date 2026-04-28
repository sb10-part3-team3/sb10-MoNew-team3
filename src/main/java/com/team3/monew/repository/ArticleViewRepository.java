package com.team3.monew.repository;

import com.team3.monew.entity.ArticleView;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  @Query("SELECT av.article.id FROM ArticleView av WHERE av.article.id IN :articleIds AND av.user.id = :userId")
  Set<UUID> findAllByArticleIdInAndUserId(
      @Param("articleIds") Collection<UUID> articleIds,
      @Param("userId") UUID userId);
}

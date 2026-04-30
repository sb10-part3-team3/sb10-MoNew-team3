package com.team3.monew.repository;

import com.team3.monew.entity.ArticleInterest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleInterestRepository extends JpaRepository<ArticleInterest, UUID> {

  List<ArticleInterest> findAllByInterestId(UUID interestId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM ArticleInterest ai WHERE ai.article.id = :articleId")
  void deleteAllByArticleId(@Param("articleId") UUID articleId);

  List<ArticleInterest> findAllByArticleId(UUID articleId);
}

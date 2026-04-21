package com.team3.monew.repository;

import com.team3.monew.entity.NewsArticle;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

  @Modifying(flushAutomatically = true)
  @Query("update NewsArticle article set article.commentCount = article.commentCount + 1 where article.id = :articleId")
  void incrementCommentCountById(@Param("articleId") UUID articleId);

  @Modifying(flushAutomatically = true)
  @Query("""
      update NewsArticle article
      set article.commentCount =
        case when article.commentCount > 0 then article.commentCount - 1 else 0 end
      where article.id = :articleId
      """)
  void decrementCommentCountById(@Param("articleId") UUID articleId);
}

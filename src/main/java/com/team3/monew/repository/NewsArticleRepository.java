package com.team3.monew.repository;

import com.team3.monew.entity.NewsArticle;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID>,
    NewsArticleRepositoryCustom {

  @Modifying(flushAutomatically = true)
  @Query("update NewsArticle article set article.commentCount = article.commentCount + 1 where article.id = :articleId")
  void incrementCommentCountById(@Param("articleId") UUID articleId);

  @Modifying(flushAutomatically = true)
  @Query("update NewsArticle article set article.viewCount = article.viewCount + 1 where article.id = :articleId")
  void incrementViewCountById(@Param("articleId") UUID articleId);

  @Modifying(flushAutomatically = true)
  @Query("""
      update NewsArticle article
      set article.commentCount =
        case when article.commentCount > 0 then article.commentCount - 1 else 0 end
      where article.id = :articleId
      """)
  void decrementCommentCountById(@Param("articleId") UUID articleId);

  @Query("""
      SELECT article.originalLink FROM NewsArticle article
      WHERE article.deleteStatus = com.team3.monew.entity.enums.DeleteStatus.ACTIVE
      AND article.originalLink IN :originalLinks
      """)
  Set<String> findExistingOriginalLinks(@Param("originalLinks") Collection<String> originalLinks);

  @Query("SELECT article FROM NewsArticle article JOIN FETCH article.source")
  List<NewsArticle> findAllWithNewsSource();

  @Query("""
      SELECT article FROM NewsArticle article
      WHERE article.publishedAt >= :startAt
        AND article.publishedAt  < :endAt
      """)
  Page<NewsArticle> findAllByPeriod(
      @Param("startAt") Instant startAt,
      @Param("endAt") Instant endAt,
      Pageable pageable);

  @Query(value = """
      SELECT CAST(article.published_at AT TIME ZONE 'Asia/Seoul' AS DATE) as localDate,
             COUNT(*) as count
      FROM news_articles article
      WHERE article.published_at >= :startAt
        AND article.published_at  < :endAt
      GROUP BY CAST(article.published_at AT TIME ZONE 'Asia/Seoul' AS DATE)
      """, nativeQuery = true)
  List<ArticleCountInfo> countNewsArticlesByPublishDate(
      @Param("startAt") Instant startAt,
      @Param("endAt") Instant endAt
  );

  interface ArticleCountInfo {

    LocalDate getLocalDate();

    Long getCount();
  }

  @Query(value = """
      SELECT article.original_link as link, article.published_at as publishedAt
      FROM news_articles article
      WHERE article.published_at >= :startAt
        AND article.published_at  < :endAt
        AND CAST(article.published_at AT TIME ZONE 'Asia/Seoul' AS DATE) IN :targetDates
      """, nativeQuery = true)
  Set<ArticleLinkAndPublishedAt> findLinksByPublishDate(
      @Param("startAt") Instant startAt,
      @Param("endAt") Instant endAt,
      @Param("targetDates") Collection<LocalDate> dates
  );

  interface ArticleLinkAndPublishedAt {

    String getLink();

    Instant getPublishedAt();
  }

  @Query("""
      SELECT a
      FROM NewsArticle a
      JOIN FETCH a.source
      WHERE a.publishedAt >= :startAt
        AND a.publishedAt  < :endAt
      """)
  List<NewsArticle> findAllByPublishedAtGreaterThanEqualAndPublishedAtLessThan(
      @Param("startAt") Instant startAt,
      @Param("endAt") Instant endAt);

}

package com.team3.monew.entity;

import com.team3.monew.entity.base.SoftDeleteEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "news_articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsArticle extends SoftDeleteEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_id", nullable = false)
  private NewsSource source;

  @Column(name = "original_link", nullable = false, unique = true, length = 1000)
  private String originalLink;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(nullable = false)
  private Instant publishedAt;

  @Column(length = 1000)
  private String summary;

  @Column(nullable = false)
  private int commentCount;

  @Column(nullable = false)
  private int viewCount;

  @OneToMany(mappedBy = "article")
  private List<Comment> comments = new ArrayList<>();

  @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ArticleInterest> articleInterests = new ArrayList<>();

  @OneToMany(mappedBy = "article")
  private List<ArticleView> articleViews = new ArrayList<>();

  public static NewsArticle create(
      NewsSource source,
      String originalLink,
      String title,
      Instant publishedAt,
      String summary
  ) {
    NewsArticle article = new NewsArticle();
    article.source = source;
    article.originalLink = originalLink;
    article.title = title;
    article.publishedAt = publishedAt;
    article.summary = summary;
    article.commentCount = 0;
    article.viewCount = 0;

    return article;
  }

  // 편의 메소드로 한번에 Article와 Interest에 각각 저장한다
  public void addArticleInterest(Interest interest, String matchedKeyword) {
    ArticleInterest articleInterest = ArticleInterest.create(this, interest, matchedKeyword);
    articleInterests.add(articleInterest);
    interest.getArticleInterests().add(articleInterest);
  }

  public void increaseViewCount() {
    this.viewCount++;
  }
}

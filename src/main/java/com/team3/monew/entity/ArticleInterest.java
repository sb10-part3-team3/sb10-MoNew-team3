package com.team3.monew.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "article_interests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_article_interests_article_id_interest_id_matched_keyword",
            columnNames = {"article_id", "interest_id", "matched_keyword"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleInterest {

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "article_id", nullable = false)
  private NewsArticle article;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "interest_id", nullable = false)
  private Interest interest;

  @Column(length = 100)
  private String matchedKeyword;

  @Column(nullable = false)
  private Instant createdAt;

  public static ArticleInterest create(
      NewsArticle article,
      Interest interest,
      String matchedKeyword
  ) {
    ArticleInterest articleInterest = new ArticleInterest();
    articleInterest.article = article;
    articleInterest.interest = interest;
    articleInterest.matchedKeyword = matchedKeyword;
    articleInterest.createdAt = Instant.now();

    return articleInterest;
  }
}
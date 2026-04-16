package com.team3.monew.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "article_views",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_article_views_article_id_user_id", columnNames = {"article_id", "user_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleView {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private NewsArticle article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant firstViewedAt;

    @Column(nullable = false)
    private Instant lastViewedAt;

    public static ArticleView create(NewsArticle article, User user) {
        ArticleView view = new ArticleView();
        view.article = article;
        view.user = user;

        Instant now = Instant.now();
        view.firstViewedAt = now;
        view.lastViewedAt = now;

        return view;
    }
}

package com.team3.monew.entity;

import com.team3.monew.entity.base.BaseEntity;
import com.team3.monew.entity.enums.NewsSourceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "news_sources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsSource extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NewsSourceType sourceType;

    @Column(length = 500)
    private String baseUrl;

    @OneToMany(mappedBy = "source")
    private List<NewsArticle> articles = new ArrayList<>();

    public static NewsSource create(
            String name,
            NewsSourceType sourceType,
            String baseUrl
    ) {
        NewsSource source = new NewsSource();
        source.name = name;
        source.sourceType = sourceType;
        source.baseUrl = baseUrl;

        return source;
    }
}
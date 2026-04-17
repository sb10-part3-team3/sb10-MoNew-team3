package com.team3.monew.repository;

import com.team3.monew.entity.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {
}

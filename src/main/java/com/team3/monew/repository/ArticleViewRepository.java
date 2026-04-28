package com.team3.monew.repository;

import com.team3.monew.entity.ArticleView;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

  Optional<ArticleView> findByArticleIdAndUserId(UUID articleId, UUID userId);
}

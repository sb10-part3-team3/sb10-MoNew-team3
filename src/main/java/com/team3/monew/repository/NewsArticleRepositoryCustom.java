package com.team3.monew.repository;

import com.team3.monew.dto.article.internal.ArticleSearchCondition;
import com.team3.monew.entity.NewsArticle;
import java.util.List;

public interface NewsArticleRepositoryCustom {

  List<NewsArticle> searchByCondition(ArticleSearchCondition condition);

  Long countByCondition(ArticleSearchCondition condition);
}

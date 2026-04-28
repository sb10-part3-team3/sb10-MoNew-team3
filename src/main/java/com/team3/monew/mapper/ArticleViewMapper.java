package com.team3.monew.mapper;

import com.team3.monew.dto.article.ArticleViewDto;
import com.team3.monew.entity.ArticleView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArticleViewMapper {

  @Mapping(target = "viewedBy", source = "articleView.user.id")
  @Mapping(target = "createdAt", source = "articleView.lastViewedAt")
  @Mapping(target = "articleId", source = "articleView.article.id")
  @Mapping(target = "source", source = "articleView.article.source.name")
  @Mapping(target = "sourceUrl", source = "articleView.article.originalLink")
  @Mapping(target = "articleTitle", source = "articleView.article.title")
  @Mapping(target = "articlePublishedDate", source = "articleView.article.publishedAt")
  @Mapping(target = "articleSummary", source = "articleView.article.summary")
  @Mapping(target = "articleCommentCount", expression = "java((long) articleView.getArticle().getCommentCount())")
  @Mapping(target = "articleViewCount", expression = "java((long) articleView.getArticle().getViewCount())")
  ArticleViewDto toArticleViewDto(ArticleView articleView);
}

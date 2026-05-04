package com.team3.monew.mapper;

import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.dto.article.ArticleDto;
import com.team3.monew.dto.article.ArticleSearchRequest;
import com.team3.monew.dto.article.internal.ArticleCursor;
import com.team3.monew.dto.article.internal.ArticleSearchCondition;
import com.team3.monew.entity.NewsArticle;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArticleMapper {

  ZoneId ASIA_SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

  @Mapping(target = "cursor", source = "cursor")
  @Mapping(target = "articleOrderBy", source = "request.orderBy")
  ArticleSearchCondition toCondition(ArticleSearchRequest request, ArticleCursor cursor);

  @Mapping(target = "source", source = "article.source.sourceType")
  @Mapping(target = "sourceUrl", source = "article.originalLink")
  @Mapping(target = "publishDate", source = "article.publishedAt")
  ArticleDto toDto(NewsArticle article, boolean viewedByMe);

  @Mapping(target = "sourceType", source = "source.sourceType")
  ArticleBackup toBackupDto(NewsArticle article);

  default Instant toInstant(LocalDateTime localDateTime) {
    if (localDateTime == null) {
      return null;
    }

    return localDateTime.atZone(ASIA_SEOUL_ZONE_ID).toInstant();
  }
}

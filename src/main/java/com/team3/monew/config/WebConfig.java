package com.team3.monew.config;

import com.team3.monew.dto.article.internal.enums.ArticleDirection;
import com.team3.monew.dto.article.internal.enums.ArticleOrderBy;
import com.team3.monew.exception.article.ArticleRequestInvalidException;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(new StringToArticleOrderByConverter());      // 대소문자 무시하는 Enum 변환자 등록
    registry.addConverter(new StringToArticleDirectionByConverter());
  }

  public static class StringToArticleOrderByConverter implements
      Converter<String, ArticleOrderBy> {

    @Override
    public ArticleOrderBy convert(@NonNull String source) {
      ArticleOrderBy articleOrderBy = ArticleOrderBy.fromValue(source);
      if (articleOrderBy == null) {
        throw new ArticleRequestInvalidException();
      }

      return articleOrderBy;
    }
  }

  public static class StringToArticleDirectionByConverter implements
      Converter<String, ArticleDirection> {

    @Override
    public ArticleDirection convert(@NonNull String source) {
      ArticleDirection direction = ArticleDirection.fromValue(source);
      if (direction == null) {
        throw new ArticleRequestInvalidException();
      }

      return direction;
    }
  }
}

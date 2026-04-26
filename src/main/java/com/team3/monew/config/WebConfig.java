package com.team3.monew.config;

import com.team3.monew.dto.article.internal.enums.Direction;
import com.team3.monew.dto.article.internal.enums.OrderBy;
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
    registry.addConverter(new StringToOrderByConverter());      // 대소문자 무시하는 Enum 변환자 등록
    registry.addConverter(new StringToDirectionByConverter());
  }

  public static class StringToOrderByConverter implements Converter<String, OrderBy> {

    @Override
    public OrderBy convert(@NonNull String source) {
      OrderBy orderBy = OrderBy.fromValue(source);
      if (orderBy == null) {
        throw new ArticleRequestInvalidException();
      }

      return orderBy;
    }
  }

  public static class StringToDirectionByConverter implements Converter<String, Direction> {

    @Override
    public Direction convert(@NonNull String source) {
      Direction direction = Direction.fromValue(source);
      if (direction == null) {
        throw new ArticleRequestInvalidException();
      }

      return direction;
    }
  }
}

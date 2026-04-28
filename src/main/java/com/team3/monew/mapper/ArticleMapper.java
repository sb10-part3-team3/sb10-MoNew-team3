package com.team3.monew.mapper;

import com.team3.monew.dto.article.ArticleViewDto;
import com.team3.monew.entity.ArticleView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleMapper {

  ArticleViewDto toArticleViewDto(ArticleView articleView);
}

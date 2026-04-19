package com.team3.monew.mapper;

import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InterestMapper {

  @Mapping(target = "keywords", source = "interest.keywords")
  @Mapping(target = "subscribedByMe", source = "subscribedByMe")
  InterestDto toDto(Interest interest, boolean subscribedByMe);

  default List<String> mapKeywords(List<InterestKeyword> keywords) {
    return keywords.stream()
        .map(InterestKeyword::getKeyword)
        .toList();
  }
}

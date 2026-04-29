package com.team3.monew.mapper;

import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.SubscriptionDto;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InterestMapper {

  @Mapping(target = "keywords", source = "interest.keywords")
  @Mapping(target = "subscribedByMe", source = "subscribedByMe")
  InterestDto toDto(Interest interest, Boolean subscribedByMe);

  @Mapping(target = "interestId", source = "interest.id")
  @Mapping(target = "interestName", source = "interest.name")
  @Mapping(target = "interestKeywords", source = "interest.keywords")
  @Mapping(target = "interestSubscriberCount", source = "interest.subscriberCount")
  @Mapping(target = "createdAt", source = "subscription.createdAt")
  @Mapping(target = "id", source = "subscription.id")
  SubscriptionDto toSubscriptionDto(Subscription subscription, Interest interest);

  default List<String> mapKeywords(List<InterestKeyword> keywords) {
    return keywords.stream()
        .map(InterestKeyword::getKeyword)
        .toList();
  }
}

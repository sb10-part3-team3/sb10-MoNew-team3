package com.team3.monew.mapper;

import com.team3.monew.document.ArticleViewSummary;
import com.team3.monew.document.CommentLikeSummary;
import com.team3.monew.document.CommentSummary;
import com.team3.monew.document.SubscriptionSummary;
import com.team3.monew.document.UserActivityDocument;
import com.team3.monew.document.UserActivityRequest;
import com.team3.monew.dto.useractivity.UserActivityDto;
import com.team3.monew.event.ArticleViewEvent;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.event.CommentRegisteredEvent;
import com.team3.monew.event.SubscriptionEvent;
import com.team3.monew.event.UserRegisteredEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserActivityMapper {

  default UserActivityDocument toDocument(UserActivityRequest request) {
    return UserActivityDocument.create(
        request.id(),
        request.email(),
        request.nickname(),
        request.createdAt());
  }

  UserActivityDto toDto(UserActivityDocument activity);

  // event -> summary
  @Mapping(target = "id", source = "userId")
  UserActivityRequest toRequest(UserRegisteredEvent event);

  @Mapping(target = "likeCount", constant = "0")
  @Mapping(target = "id", source = "commentId")
  CommentSummary toCommentSummary(CommentRegisteredEvent event);

  @Mapping(target = "id", source = "subscriptionId")
  SubscriptionSummary toSubscriptionSummary(SubscriptionEvent event);

  @Mapping(target = "commentUserId", source = "actorUserId")
  @Mapping(target = "createdAt", source = "commentCreatedAt")
  CommentLikeSummary toCommentLikeSummary(CommentLikedEvent event);

  @Mapping(target = "viewedBy", source = "userId")
  ArticleViewSummary toArticleViewSummary(ArticleViewEvent event);
}

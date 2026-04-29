package com.team3.monew.repository;

import com.team3.monew.document.UserActivityDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface UserActivityRepository extends MongoRepository<UserActivityDocument, UUID> {

  @Query("{ 'articleViews.articleId': ?0 }")
  List<UserActivityDocument> findAllByArticleViewsArticleId(UUID articleId);

  @Query("{ 'subscriptions.interestId': ?0} ")
  List<UserActivityDocument> findAllBySubscriptionsInterestId(UUID interestId);

  @Query("{ 'comments.userId':  ?0} ")
  List<UserActivityDocument> findAllByCommentsUserId(UUID userId);
}

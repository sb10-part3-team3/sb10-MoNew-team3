package com.team3.monew.repository;

import com.team3.monew.document.UserActivityDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface UserActivityRepository extends MongoRepository<UserActivityDocument, UUID>, UserActivityRepositoryCustom {

  @Query("{ 'articleViews.articleId': ?0 }")
  List<UserActivityDocument> findAllByArticleViewsArticleId(UUID articleId);

  @Query("{ 'subscriptions.interestId': ?0} ")
  List<UserActivityDocument> findAllBySubscriptionsInterestId(UUID interestId);

  @Query("{ 'comments.userId':  ?0} ")
  List<UserActivityDocument> findAllByCommentsUserId(UUID userId);

  @Query("{ 'commentLikes.commentId': ?0 }")
  List<UserActivityDocument> findAllByCommentLikesCommentId(UUID commentId);

  @Query("{ 'commentLikes.commentId':  { $in:  ?0}}")
  List<UserActivityDocument> findAllByCommentLikesCommentIdIn(List<UUID> commentIds);

  void deleteByIdIn(List<UUID> ids);

  @Query("{ 'subscriptions.interestId': ?0 }")
  @Update("{ '$inc': { 'subscriptions.$.interestSubscriberCount': ?1 } }")
  void incrementSubscriberCount(UUID interestId, int delta);

  @Query("{ 'comments.id': ?0 }")
  @Update("{ '$inc': { 'comments.$.likeCount': ?1 } }")
  void incrementCommentLikeCount(UUID commentId, int delta);

  @Query("{ 'commentLikes.commentId': ?0 }")
  @Update("{ '$inc': { 'commentLikes.$.commentLikeCount': ?1 } }")
  void incrementCommentLikeCountInLikes(UUID commentId, int delta);
}

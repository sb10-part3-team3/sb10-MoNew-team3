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

  @Query("{ 'articleViews.articleId': ?0 }")
  @Update("{ '$inc': { 'articleViews.$.articleCommentCount': ?1 } }")
  void incrementArticleCommentCount(UUID articleId, int delta);

  @Query("{ 'articleViews.articleId': ?0 }")
  @Update("{ '$inc': { 'articleViews.$.articleViewCount': ?1 } }")
  void incrementArticleViewCount(UUID articleId, int delta);

  @Query("{ 'commentLikes.commentUserId': ?0 }")
  List<UserActivityDocument> findAllByCommentLikesCommentUserId(UUID commentUserId);

  @Query("{ 'commentLikes.commentId': ?0 }")
  @Update("{ '$pull': { 'commentLikes': { 'commentId': ?0 } } }")
  void removeCommentLikeSummaryByCommentId(UUID commentId);

  @Query("{ 'commentLikes.commentId': { $in: ?0 } }")
  @Update("{ '$pull': { 'commentLikes': { 'commentId': { $in: ?0 } } } }")
  void removeCommentLikeSummariesByCommentIds(List<UUID> commentIds);

  // articleViews에서 해당 기사 항목 일괄 제거
  @Query("{ 'articleViews.articleId': ?0 }")
  @Update("{ '$pull': { 'articleViews': { 'articleId': ?0 } } }")
  void removeArticleViewSummaryByArticleId(UUID articleId);

  // comments에서 해당 기사 댓글 일괄 제거
  @Query("{ 'comments.articleId': ?0 }")
  @Update("{ '$pull': { 'comments': { 'articleId': ?0 } } }")
  void removeCommentSummaryByArticleId(UUID articleId);

  // commentLikes에서 해당 기사 댓글 좋아요 일괄 제거
  @Query("{ 'commentLikes.articleId': ?0 }")
  @Update("{ '$pull': { 'commentLikes': { 'articleId': ?0 } } }")
  void removeCommentLikeSummaryByArticleId(UUID articleId);

  @Query("{ 'subscriptions.interestId': ?0 }")
  @Update("{ '$set': { 'subscriptions.$.interestSubscriberCount': ?1 } }")
  void updateSubscriberCount(UUID interestId, int count);

  @Query("{ 'comments.id': ?0 }")
  @Update("{ '$set': { 'comments.$.likeCount': ?1 } }")
  void updateCommentLikeCount(UUID commentId, int count);

  @Query("{ 'commentLikes.commentId': ?0 }")
  @Update("{ '$set': { 'commentLikes.$.commentLikeCount': ?1 } }")
  void updateCommentLikeCountInLikes(UUID commentId, int count);

  @Query("{ 'articleViews.articleId': ?0 }")
  @Update("{ '$set': { 'articleViews.$.articleViewCount': ?1 } }")
  void updateArticleViewCount(UUID articleId, int count);
}

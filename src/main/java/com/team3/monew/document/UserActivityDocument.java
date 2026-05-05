package com.team3.monew.document;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "user_activities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActivityDocument {

  // 낙관적 락을 위한 버전 필드
  // MongoDB가 자동으로 체크
  @Version
  private Long version;

  @Id
  private UUID id;
  private String email;
  private String nickname;
  private Instant createdAt;

  private List<SubscriptionSummary> subscriptions = new ArrayList<>();

  @Size(max = 10)
  private List<CommentSummary> comments = new ArrayList<>();

  @Size(max = 10)
  private List<CommentLikeSummary> commentLikes = new ArrayList<>();

  @Size(max = 10)
  private List<ArticleViewSummary> articleViews = new ArrayList<>();

  public static UserActivityDocument create(UUID id, String email, String nickname, Instant createdAt) {
    UserActivityDocument userActivityDocument = new UserActivityDocument();
    userActivityDocument.id = id;
    userActivityDocument.email = email;
    userActivityDocument.nickname = nickname;
    userActivityDocument.createdAt = createdAt;
    return userActivityDocument;
  }

  // 이벤트 순서 꼬일 시 임시 생성 용
  public static UserActivityDocument empty(UUID userId) {
    UserActivityDocument userActivityDocument = new UserActivityDocument();
    userActivityDocument.id = userId;
    userActivityDocument.email = null;
    userActivityDocument.nickname = null;
    userActivityDocument.createdAt = Instant.now();
    return userActivityDocument;
  }

  public void addSubscriptionSummary(SubscriptionSummary subscriptionSummary) {
    subscriptions.removeIf(s -> Objects.equals(s.id(), subscriptionSummary.id()));
    subscriptions.add(subscriptionSummary);
  }

  public void addCommentSummary(CommentSummary commentSummary) {
    addToRecentList(comments, commentSummary, CommentSummary::id);
  }

  public void addCommentLikeSummary(CommentLikeSummary commentLikeSummary) {
    addToRecentList(commentLikes, commentLikeSummary, CommentLikeSummary::id);
  }

  public void addArticleViewSummary(ArticleViewSummary articleViewSummary) {
    addToRecentList(articleViews, articleViewSummary, ArticleViewSummary::id);
  }

  public void updateUserInfo(String email, String nickname, Instant createdAt) {
    this.email = email;
    this.nickname = nickname;
    this.createdAt = createdAt;
  }

  public void updateNickname(String newNickname) {
    this.nickname = newNickname;
  }

  public void updateCommentNickname(String newNickname) {
    comments = comments.stream()
        .map(comment -> Objects.equals(comment.userId(), this.id)
                ? new CommentSummary(
                comment.id(),
                comment.articleId(),
                comment.articleTitle(),
                comment.userId(),
                newNickname,
                comment.content(),
                comment.likeCount(),
                comment.createdAt()
            ) : comment
        ).collect(Collectors.toCollection(ArrayList::new));
  }

  public void removeCommentSummary(UUID commentId) {
    comments.removeIf(c -> Objects.equals(c.id(), commentId));
  }

  public void updateCommentContent(UUID commentId, String newContent) {
    comments = comments.stream()
        .map(comment -> Objects.equals(comment.id(), commentId)
            ? new CommentSummary(
            comment.id(),
            comment.articleId(),
            comment.articleTitle(),
            comment.userId(),
            comment.userNickname(),
            newContent,
            comment.likeCount(),
            comment.createdAt()
            ) : comment
        ).collect(Collectors.toCollection(ArrayList::new));
  }

  public void removeArticleViewSummary(UUID articleId) {
    articleViews.removeIf(articleView -> Objects.equals(articleView.articleId(), articleId));
  }

  public void removeCommentLikeSummary(UUID commentLikeId) {
    commentLikes.removeIf(commentLike -> Objects.equals(commentLike.id(), commentLikeId));
  }

  public void removeSubscriptionSummary(UUID subscriptionId) {
    subscriptions.removeIf(subscription -> Objects.equals(subscription.id(), subscriptionId));
  }

  public void removeSubscriptionSummaryByInterestId(UUID interestId) {
    subscriptions.removeIf(subscription -> Objects.equals(subscription.interestId(), interestId));
  }

  public void updateKeywords(UUID interestId, List<String> keywords) {
    subscriptions = subscriptions.stream()
        .map(subscription -> subscription.interestId().equals(interestId)
            ? new SubscriptionSummary(
            subscription.id(),
            subscription.interestId(),
            subscription.interestName(),
            List.copyOf(keywords),
            subscription.interestSubscriberCount(),
            subscription.createdAt()
            ) : subscription
        ).collect(Collectors.toCollection(ArrayList::new));
  }

  public void removeCommentLikeSummaryByCommentId(UUID commentId) {
    commentLikes.removeIf(commentLike -> Objects.equals(commentLike.commentId(), commentId));
  }

  public void updateCommentLikeContent(UUID commentId, String newContent) {
    commentLikes = commentLikes.stream()
        .map(commentLike -> commentLike.commentId().equals(commentId)
            ? new CommentLikeSummary(
            commentLike.id(),
            commentLike.createdAt(),
            commentLike.commentId(),
            commentLike.articleId(),
            commentLike.articleTitle(),
            commentLike.commentUserId(),
            commentLike.commentUserNickname(),
            newContent,
            commentLike.commentLikeCount(),
            commentLike.commentCreatedAt()
            ) : commentLike
        ).collect(Collectors.toCollection(ArrayList::new));
  }

  public void updateCommentLikeNickname(UUID commentUserId, String newNickname) {
    commentLikes = commentLikes.stream()
        .map(commentLike -> Objects.equals(commentLike.commentUserId(), commentUserId)
            ? new CommentLikeSummary(
            commentLike.id(),
            commentLike.createdAt(),
            commentLike.commentId(),
            commentLike.articleId(),
            commentLike.articleTitle(),
            commentLike.commentUserId(),
            newNickname,
            commentLike.commentContent(),
            commentLike.commentLikeCount(),
            commentLike.commentCreatedAt()
            ) : commentLike
        ).collect(Collectors.toCollection(ArrayList::new));
  }

  private <T, ID> void addToRecentList(List<T> items, T newItem, Function<T, ID> idExtractor) {
    // 중복 제거
    items.removeIf(item -> Objects.equals(idExtractor.apply(item), idExtractor.apply(newItem)));
    items.add(0, newItem);

    if (items.size() > 10) {
      items.remove(items.size() - 1);
    }
  }
}

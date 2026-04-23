package com.team3.monew.document;

import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "user_activities")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActivityDocument {

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

  public void addSubscriptionSummary(SubscriptionSummary subscriptionSummary) {
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

  // 삭제는 추후 구현


  private <T, ID> void addToRecentList(List<T> items, T newItem, Function<T, ID> idExtractor) {
    // 중복 제거
    items.removeIf(item -> Objects.equals(idExtractor.apply(item), idExtractor.apply(newItem)));
    items.add(0, newItem);

    if (items.size() > 10) {
      items.remove(items.size() - 1);
    }
  }
}

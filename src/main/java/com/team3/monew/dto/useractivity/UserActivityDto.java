package com.team3.monew.dto.useractivity;

import com.team3.monew.document.ArticleViewSummary;
import com.team3.monew.document.CommentLikeSummary;
import com.team3.monew.document.CommentSummary;
import com.team3.monew.document.SubscriptionSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserActivityDto(
    UUID id,
    String email,
    String nickname,
    Instant createdAt,
    List<SubscriptionSummary> subscriptions,
    List<CommentSummary> comments,
    List<CommentLikeSummary> commentLikes,
    List<ArticleViewSummary> articleViews
) {

}

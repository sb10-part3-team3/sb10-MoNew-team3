package com.team3.monew.listener;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.team3.monew.document.ArticleViewSummary;
import com.team3.monew.document.CommentLikeSummary;
import com.team3.monew.document.CommentSummary;
import com.team3.monew.document.SubscriptionSummary;
import com.team3.monew.document.UserActivityRequest;
import com.team3.monew.event.ArticleViewEvent;
import com.team3.monew.event.CommentLikedEvent;
import com.team3.monew.event.CommentRegisteredEvent;
import com.team3.monew.event.SubscriptionEvent;
import com.team3.monew.event.UserRegisteredEvent;
import com.team3.monew.mapper.UserActivityMapper;
import com.team3.monew.service.UserActivityService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class UserActivityEventListenerTest {

  @Mock
  private UserActivityService userActivityService;

  @Mock
  private UserActivityMapper userActivityMapper;

  @InjectMocks
  private UserActivityEventListener userActivityEventListener;

  @Test
  @DisplayName("리스너가 사용자 등록 이벤트를 받으면 서비스의 사용자 활동 등록 메서드를 호출한다.")
  void shouldCallRegisterUserActivityMethod_whenListenUserRegisteredEvent() {
    // given
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.now();

    UserRegisteredEvent event = new UserRegisteredEvent(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    UserActivityRequest request = new UserActivityRequest(
        userId,
        "test@test.com",
        "tester",
        createdAt
    );

    given(userActivityMapper.toRequest(event)).willReturn(request);

    // when
    userActivityEventListener.handleUserRegisterEvent(event);

    // then
    then(userActivityMapper).should(times(1)).toRequest(event);
    then(userActivityService).should(times(1)).registerUserActivity(request);
  }

  @Test
  @DisplayName("리스너가 구독 이벤트를 받으면 서비스의 구독 요약 갱신 메서드를 호출한다.")
  void shouldCallUpdateSubscriptionSummaryMethod_whenListenSubscriptionEvent() {
    // given
    UUID userId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();
    Instant createdAt = Instant.now();

    SubscriptionEvent event = new SubscriptionEvent(
        subscriptionId,
        interestId,
        userId,
        "경제",
        List.of("금리", "주식"),
        10,
        createdAt
    );

    SubscriptionSummary summary = new SubscriptionSummary(
        subscriptionId,
        interestId,
        "경제",
        List.of("금리", "주식"),
        10,
        createdAt
    );

    given(userActivityMapper.toSubscriptionSummary(event)).willReturn(summary);

    // when
    userActivityEventListener.handleSubscriptionEvent(event);

    // then
    then(userActivityMapper).should(times(1)).toSubscriptionSummary(event);
    then(userActivityService).should(times(1))
        .updateSubscriptionSummary(userId, summary);
  }

  @Test
  @DisplayName("리스너가 댓글 등록 이벤트를 받으면 서비스의 댓글 요약 갱신 메서드를 호출한다.")
  void shouldCallUpdateCommentSummaryMethod_whenListenCommentRegisteredEvent() {
    // given
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    Instant createdAt = Instant.now();

    CommentRegisteredEvent event = new CommentRegisteredEvent(
        commentId,
        articleId,
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0,
        createdAt
    );

    CommentSummary summary = new CommentSummary(
        commentId,
        articleId,
        "기사 제목",
        userId,
        "tester",
        "댓글 내용",
        0,
        createdAt
    );

    given(userActivityMapper.toCommentSummary(event)).willReturn(summary);

    // when
    userActivityEventListener.handleCommentRegisteredEvent(event);

    // then
    then(userActivityMapper).should(times(1)).toCommentSummary(event);
    then(userActivityService).should(times(1)).updateCommentSummary(summary);
  }

  @Test
  @DisplayName("리스너가 댓글 좋아요 이벤트를 받으면 서비스의 댓글 좋아요 요약 갱신 메서드를 호출한다.")
  void shouldCallUpdateCommentLikeSummaryMethod_whenListenCommentLikedEvent() {
    // given
    UUID actorUserId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID commentLikeId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    UUID commentUserId = UUID.randomUUID();
    Instant createdAt = Instant.now();

    CommentLikedEvent event = new CommentLikedEvent(
        actorUserId,
        commentId,
        commentLikeId,
        articleId,
        "기사 제목",
        commentUserId,
        "commentWriter",
        "댓글 내용",
        1,
        createdAt
    );

    CommentLikeSummary summary = new CommentLikeSummary(
        commentLikeId,
        createdAt,
        commentId,
        articleId,
        "기사 제목",
        commentUserId,
        "commentWriter",
        "댓글 내용",
        1,
        createdAt
    );

    given(userActivityMapper.toCommentLikeSummary(event)).willReturn(summary);

    // when
    userActivityEventListener.handleCommentLikedEvent(event);

    // then
    then(userActivityMapper).should(times(1)).toCommentLikeSummary(event);
    then(userActivityService).should(times(1))
        .updateCommentLikeSummary(actorUserId, summary);
  }

  @Test
  @DisplayName("리스너가 기사 조회 이벤트를 받으면 서비스의 기사 조회 요약 갱신 메서드를 호출한다.")
  void shouldCallUpdateArticleViewSummaryMethod_whenListenArticleViewEvent() {
    // given
    UUID userId = UUID.randomUUID();
    UUID articleViewId = UUID.randomUUID();
    UUID articleId = UUID.randomUUID();
    Instant createdAt = Instant.now();
    Instant publishedAt = Instant.now();

    ArticleViewEvent event = new ArticleViewEvent(
        articleViewId,
        userId,
        createdAt,
        articleId,
        "NAVER",
        "https://example.com",
        "기사 제목",
        publishedAt,
        "기사 요약",
        3,
        100
    );

    ArticleViewSummary summary = new ArticleViewSummary(
        articleViewId,
        userId,
        createdAt,
        articleId,
        "NAVER",
        "https://example.com",
        "기사 제목",
        publishedAt,
        "기사 요약",
        3,
        100
    );

    given(userActivityMapper.toArticleViewSummary(event)).willReturn(summary);

    // when
    userActivityEventListener.handleArticleViewEvent(event);

    // then
    then(userActivityMapper).should(times(1)).toArticleViewSummary(event);
    then(userActivityService).should(times(1))
        .updateArticleViewSummary(userId, summary);
  }
}
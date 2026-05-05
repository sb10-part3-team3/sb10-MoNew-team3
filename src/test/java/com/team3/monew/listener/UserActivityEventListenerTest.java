package com.team3.monew.listener;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.team3.monew.document.ArticleViewSummary;
import com.team3.monew.document.CommentLikeSummary;
import com.team3.monew.document.CommentSummary;
import com.team3.monew.document.SubscriptionSummary;
import com.team3.monew.document.UserActivityRequest;
import com.team3.monew.event.ArticleDeletedEvent;
import com.team3.monew.event.ArticleViewEvent;
import com.team3.monew.event.CommentDeletedEvent;
import com.team3.monew.event.CommentLikedActivityEvent;
import com.team3.monew.event.CommentRegisteredEvent;
import com.team3.monew.event.CommentUnlikedEvent;
import com.team3.monew.event.CommentUpdatedEvent;
import com.team3.monew.event.InterestDeletedEvent;
import com.team3.monew.event.InterestKeywordUpdatedEvent;
import com.team3.monew.event.SubscriptionCanceledEvent;
import com.team3.monew.event.SubscriptionEvent;
import com.team3.monew.event.UserDeletedEvent;
import com.team3.monew.event.UserRegisteredEvent;
import com.team3.monew.event.UserUpdatedEvent;
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

    CommentLikedActivityEvent event = new CommentLikedActivityEvent(
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
        100,
        true
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
        .updateArticleViewSummary(userId, summary, true);
  }

  @Test
  @DisplayName("리스너가 사용자 삭제 이벤트를 받으면 서비스의 사용자 활동 내역 삭제 메서드를 호출한다.")
  void shouldCallDeleteUserActivityMethod_whenListenUserDeletedEvent() {
    // given
    UUID userId = UUID.randomUUID();

    UserDeletedEvent event = new UserDeletedEvent(userId);

    // when
    userActivityEventListener.handleUserDeletedEvent(event);

    // then
    then(userActivityService).should(times(1)).deleteUserActivity(userId);
  }

  @Test
  @DisplayName("리스너가 사용자 수정 이벤트를 받으면 서비스의 닉네임 업데이트 메서드를 호출한다.")
  void shouldCallUpdateUserNicknameMethod_whenListenUserUpdatedEvent() {
    // given
    UUID userId = UUID.randomUUID();
    String newNickname = "newTester";

    UserUpdatedEvent event = new UserUpdatedEvent(userId, newNickname);

    // when
    userActivityEventListener.handleUserUpdatedEvent(event);

    // then
    then(userActivityService).should(times(1)).updateUserNickname(userId, newNickname);
  }

  @Test
  @DisplayName("리스너가 댓글 삭제 이벤트를 받으면 서비스의 댓글 삭제 메서드를 호출한다.")
  void shouldCallRemoveCommentSummaryMethod_whenListenCommentDeletedEvent() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    CommentDeletedEvent event = new CommentDeletedEvent(commentId, userId);

    // when
    userActivityEventListener.handleCommentDeletedEvent(event);

    // then
    then(userActivityService).should(times(1))
        .removeCommentSummary(userId, commentId);
  }

  @Test
  @DisplayName("리스너가 댓글 수정 이벤트를 받으면 서비스의 댓글 수정 메서드를 호출한다.")
  void shouldCallUpdateCommentContentMethod_whenListenCommentUpdatedEvent() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String newContent = "수정된 댓글 내용";

    CommentUpdatedEvent event = new CommentUpdatedEvent(commentId, userId, newContent);

    // when
    userActivityEventListener.handleCommentUpdatedEvent(event);

    // then
    then(userActivityService).should(times(1))
        .updateCommentContent(userId, commentId, newContent);
  }

  @Test
  @DisplayName("리스너가 댓글 좋아요 취소 이벤트를 받으면 서비스의 댓글 좋아요 삭제 메서드를 호출한다.")
  void shouldCallRemoveCommentLikeSummaryMethod_whenListenCommentUnlikedEvent() {
    // given
    UUID commentLikeId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    CommentUnlikedEvent event = new CommentUnlikedEvent(userId, commentLikeId, commentId);

    // when
    userActivityEventListener.handleCommentUnlikedEvent(event);

    // then
    then(userActivityService).should(times(1))
        .removeCommentLikeSummary(userId, commentLikeId, commentId);
  }

  @Test
  @DisplayName("리스너가 기사 삭제 이벤트를 받으면 서비스의 기사 뷰 삭제 메서드를 호출한다.")
  void shouldCallRemoveArticleViewSummaryMethod_whenListenArticleDeletedEvent() {
    // given
    UUID articleId = UUID.randomUUID();

    ArticleDeletedEvent event = new ArticleDeletedEvent(articleId);

    // when
    userActivityEventListener.handleArticleDeletedEvent(event);

    // then
    then(userActivityService).should(times(1))
        .removeArticleViewSummary(articleId);
  }

  @Test
  @DisplayName("리스너가 관심사 삭제 이벤트를 받으면 서비스의 관심사의 구독 모든 구독 삭제 메서드를 호출한다.")
  void shouldCallRemoveSubscriptionSummaryMethod_whenListenSubscriptionCanceledEvent() {
    // given
    UUID subscriptionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    SubscriptionCanceledEvent event = new SubscriptionCanceledEvent(userId, subscriptionId);

    // when
    userActivityEventListener.handleSubscriptionCanceledEvent(event);

    // then
    then(userActivityService).should(times(1))
        .removeSubscriptionSummary(userId, subscriptionId);
  }

  @Test
  @DisplayName("리스너가 구독 취소 이벤트를 받으면 서비스의 구독 요약 삭제 메서드를 호출한다.")
  void shouldCallRemoveAllSubscriptionSummaryByInterestMethod_whenListenInterestDeletedEvent() {
    // given
    UUID interestId = UUID.randomUUID();

    InterestDeletedEvent event = new InterestDeletedEvent(interestId);

    // when
    userActivityEventListener.handleInterestDeletedEvent(event);

    // then
    then(userActivityService).should(times(1))
        .removeAllSubscriptionSummaryByInterest(interestId);
  }

  @Test
  @DisplayName("리스너가 관심사 키워드 수정 이벤트를 받으면 활동 내역 서비스의 관심사의 키워드 수정 메서드를 호출한다.")
  void shouldCallUpdateSubscriptionsByKeywordsByInterestMethod_whenListenInterestKeywordUpdatedEvent() {
    // given
    UUID interestId = UUID.randomUUID();
    List<String> keywords = List.of("keyword1", "keyword2");

    InterestKeywordUpdatedEvent event = new InterestKeywordUpdatedEvent(interestId, keywords);

    // when
    userActivityEventListener.handleInterestKeywordUpdatedEvent(event);

    // then
    then(userActivityService).should(times(1))
        .updateSubscriptionsByKeywords(interestId, keywords);
  }
}
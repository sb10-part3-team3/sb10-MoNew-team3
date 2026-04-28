package com.team3.monew.service;

import com.team3.monew.dto.article.ArticleViewDto;
import com.team3.monew.entity.ArticleView;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.DeleteStatus;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.event.ArticleViewEvent;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.exception.user.DeletedUserException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

  @Mock
  private NewsArticleRepository newsArticleRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ArticleViewRepository articleViewRepository;

  @Mock
  private ArticleMapper articleMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private ArticleService articleService;

  private UUID articleId;
  private UUID userId;
  private NewsArticle article;
  private User user;

  @BeforeEach
  void setUp() {
    articleId = UUID.randomUUID();
    userId = UUID.randomUUID();

    NewsSource source = NewsSource.create("NAVER", NewsSourceType.NAVER, "https://news.naver.com");
    article = NewsArticle.create(
        source,
        "https://news.naver.com/article/1",
        "테스트 기사",
        Instant.parse("2026-04-28T00:00:00Z"),
        "테스트 기사 요약"
    );
    user = User.create("user@example.com", "테스터", "encoded-password");

    assignId(source, UUID.randomUUID());
    assignId(article, articleId);
    assignId(user, userId);
  }

  @Nested
  @DisplayName("기사 뷰 등록 기능을 검증한다.")
  class RegisterArticleView {

    @Test
    @DisplayName("사용자가 기사를 처음 조회하면 조회 기록을 저장하고 기사 조회 수를 1 증가시킨다.")
    void shouldRegisterArticleViewAndIncreaseViewCount_whenFirstViewRequest() {
      // given
      UUID articleViewId = UUID.randomUUID();
      Instant viewedAt = Instant.parse("2026-04-28T10:15:30Z");

      ArticleView savedArticleView = ArticleView.create(article, user);
      assignId(savedArticleView, articleViewId);
      ReflectionTestUtils.setField(savedArticleView, "firstViewedAt", viewedAt);
      ReflectionTestUtils.setField(savedArticleView, "lastViewedAt", viewedAt);

      ArticleViewDto expected = new ArticleViewDto(
          articleViewId,
          userId,
          viewedAt,
          articleId,
          "NAVER",
          article.getOriginalLink(),
          article.getTitle(),
          article.getPublishedAt(),
          article.getSummary(),
          article.getCommentCount(),
          1L
      );

      given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(articleViewRepository.findByArticleIdAndUserId(articleId, userId)).willReturn(Optional.empty());
      given(articleViewRepository.save(any(ArticleView.class))).willReturn(savedArticleView);
      given(articleMapper.toArticleViewDto(any(ArticleView.class))).willReturn(expected);

      // when
      ArticleViewDto actual = articleService.registerArticleView(articleId, userId);

      // then
      assertThat(actual).isEqualTo(expected);
      assertThat(article.getViewCount()).isEqualTo(1);

      then(articleViewRepository).should().save(argThat(articleView ->
          articleView.getArticle() == article
              && articleView.getUser() == user
      ));
      then(eventPublisher).should().publishEvent(argThat((Object event) -> {
        if (!(event instanceof ArticleViewEvent articleViewEvent)) {
          return false;
        }

        return articleViewEvent.id().equals(articleViewId)
            && articleViewEvent.userId().equals(userId)
            && articleViewEvent.articleId().equals(articleId)
            && Integer.valueOf(1).equals(articleViewEvent.articleViewCount());
      }));
      then(articleMapper).should().toArticleViewDto(any(ArticleView.class));
    }

    @Test
    @DisplayName("이미 조회한 기사를 다시 조회하면 조회 기록 시각만 갱신하고 기사 조회 수는 증가시키지 않는다.")
    void shouldRefreshLastViewedAtWithoutIncreasingViewCount_whenArticleViewAlreadyExists() {
      // given
      UUID articleViewId = UUID.randomUUID();
      Instant firstViewedAt = Instant.parse("2026-04-20T09:00:00Z");
      Instant previousLastViewedAt = Instant.parse("2026-04-20T10:00:00Z");

      ArticleView existingArticleView = ArticleView.create(article, user);
      assignId(existingArticleView, articleViewId);
      ReflectionTestUtils.setField(existingArticleView, "firstViewedAt", firstViewedAt);
      ReflectionTestUtils.setField(existingArticleView, "lastViewedAt", previousLastViewedAt);
      ReflectionTestUtils.setField(article, "viewCount", 5);

      ArticleViewDto expected = new ArticleViewDto(
          articleViewId,
          userId,
          previousLastViewedAt,
          articleId,
          "NAVER",
          article.getOriginalLink(),
          article.getTitle(),
          article.getPublishedAt(),
          article.getSummary(),
          article.getCommentCount(),
          5L
      );

      given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(articleViewRepository.findByArticleIdAndUserId(articleId, userId))
          .willReturn(Optional.of(existingArticleView));
      given(articleMapper.toArticleViewDto(any(ArticleView.class))).willReturn(expected);

      // when
      ArticleViewDto actual = articleService.registerArticleView(articleId, userId);

      // then
      assertThat(actual).isEqualTo(expected);
      assertThat(article.getViewCount()).isEqualTo(5);
      assertThat(existingArticleView.getLastViewedAt()).isAfter(previousLastViewedAt);

      then(articleViewRepository).should(never()).save(any(ArticleView.class));
      then(eventPublisher).should().publishEvent(argThat((Object event) -> {
        if (!(event instanceof ArticleViewEvent articleViewEvent)) {
          return false;
        }

        return articleViewEvent.id().equals(articleViewId)
            && articleViewEvent.userId().equals(userId)
            && articleViewEvent.articleId().equals(articleId)
            && Integer.valueOf(5).equals(articleViewEvent.articleViewCount());
      }));
      then(articleMapper).should().toArticleViewDto(any(ArticleView.class));
    }

    @Test
    @DisplayName("존재하지 않는 기사를 조회하면 기사 없음 예외가 발생한다.")
    void shouldThrowArticleNotFoundException_whenArticleDoesNotExist() {
      // given
      given(newsArticleRepository.findById(articleId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId))
          .isInstanceOf(ArticleNotFoundException.class);

      then(userRepository).shouldHaveNoInteractions();
      then(articleViewRepository).shouldHaveNoInteractions();
      then(articleMapper).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("삭제된 기사를 조회하면 삭제된 기사 예외가 발생한다.")
    void shouldThrowDeletedArticleException_whenArticleIsDeleted() {
      // given
      markDeleted(article);
      given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));

      // when & then
      assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId))
          .isInstanceOf(DeletedArticleException.class);

      then(userRepository).shouldHaveNoInteractions();
      then(articleViewRepository).shouldHaveNoInteractions();
      then(articleMapper).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 기사를 조회하면 사용자 없음 예외가 발생한다.")
    void shouldThrowUserNotFoundException_whenUserDoesNotExist() {
      // given
      given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId))
          .isInstanceOf(UserNotFoundException.class);

      then(articleViewRepository).shouldHaveNoInteractions();
      then(articleMapper).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("삭제된 사용자가 기사를 조회하면 삭제된 사용자 예외가 발생한다.")
    void shouldThrowDeletedUserException_whenUserIsDeleted() {
      // given
      markDeleted(user);
      given(newsArticleRepository.findById(articleId)).willReturn(Optional.of(article));
      given(userRepository.findById(userId)).willReturn(Optional.of(user));

      // when & then
      assertThatThrownBy(() -> articleService.registerArticleView(articleId, userId))
          .isInstanceOf(DeletedUserException.class);

      then(articleViewRepository).shouldHaveNoInteractions();
      then(articleMapper).shouldHaveNoInteractions();
      then(eventPublisher).shouldHaveNoInteractions();
    }
  }

  private void assignId(Object entity, UUID id) {
    ReflectionTestUtils.setField(entity, "id", id);
  }

  private void markDeleted(Object entity) {
    ReflectionTestUtils.setField(entity, "deleteStatus", DeleteStatus.DELETED);
    ReflectionTestUtils.setField(entity, "deletedAt", Instant.parse("2026-04-28T00:00:00Z"));
  }
}

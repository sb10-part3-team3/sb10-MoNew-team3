package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team3.monew.dto.notification.CommentLikedNotificationRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.UserRepository;
import com.team3.monew.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class NotificationIntegrationTest {

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  @Autowired
  private NewsSourceRepository newsSourceRepository;

  private User writer;
  private User actor;
  private Comment comment;
  private NewsArticle article;

  @BeforeEach
  void setUp() {
    //비동기이기 때문에 트랜잭셔널 별도임으로 개별 삭제
    notificationRepository.deleteAll();
    commentRepository.deleteAll();
    newsArticleRepository.deleteAll();
    newsSourceRepository.deleteAll();
    userRepository.deleteAll();

    writer = userRepository.save(User.create("writer@test.com", "writer", "pwd"));
    actor = userRepository.save(User.create("actor@test.com", "actor", "pwd"));
    NewsSource newsSource = NewsSource.create("test", NewsSourceType.NAVER, "url");
    newsSourceRepository.save(newsSource);
    article = newsArticleRepository.save(
        NewsArticle.create(newsSource, "link", "title", Instant.now(), "summary"));
    comment = commentRepository.save(Comment.create(article, writer, "댓글 내용"));
  }

  @Test
  @DisplayName("댓글 좋아요 알림 등록 요청 시, 알림이 저장된다.")
  void shouldRegisterNotification_whenRequestCommentLikedNotification() {
    // given
    CommentLikedNotificationRequest request = new CommentLikedNotificationRequest(
        actor.getId(), comment.getId());

    // when
    notificationService.registerLikeNotification(request);

    // then
    List<Notification> results = notificationRepository.findAll();
    assertThat(results).hasSize(1);
    Notification saved = results.get(0);
    assertThat(saved.getResourceType()).isEqualTo(NotificationResourceType.COMMENT);
    assertThat(saved.getContent()).contains(actor.getNickname());
    assertThat(saved.getUser().getId()).isEqualTo(writer.getId());
    assertThat(saved.getResourceId()).isEqualTo(comment.getId());
    assertThat(saved.getActorUser().getId()).isEqualTo(actor.getId());
    assertThat(saved.isConfirmed()).isFalse();
    assertThat(saved.getConfirmedAt()).isNull();
  }

  @Test
  @DisplayName("존재하지 않는 댓글 ID로 알림 등록을 요청하면 예외가 발생한다.")
  void shouldThrowException_whenCommentNotFound() {
    // given
    UUID nonExistCommentId = UUID.randomUUID();
    CommentLikedNotificationRequest request = new CommentLikedNotificationRequest(
        actor.getId(), nonExistCommentId);

    // When & Then
    assertThatThrownBy(() -> notificationService.registerLikeNotification(request))
        .isInstanceOf(CommentNotFoundException.class);
    List<Notification> results = notificationRepository.findAll();
    assertThat(results).hasSize(0);
  }

  @Test
  @DisplayName("존재하지 않는 좋아요 사용자 ID로 알림 등록을 요청하면 예외가 발생한다.")
  void shouldThrowException_whenActorUserNotFound() {
    // given
    UUID nonExistActorId = UUID.randomUUID();
    CommentLikedNotificationRequest request = new CommentLikedNotificationRequest(
        nonExistActorId, comment.getId());

    // When & Then
    assertThatThrownBy(() -> notificationService.registerLikeNotification(request))
        .isInstanceOf(UserNotFoundException.class);
    List<Notification> results = notificationRepository.findAll();
    assertThat(results).hasSize(0);
  }
}

package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.team3.monew.dto.notification.CommentLikedNotificationRequest;
import com.team3.monew.dto.notification.InterestNotificationRequest;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.exception.comment.CommentNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.InterestRepository;
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
import org.junit.jupiter.api.Nested;
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

  @Autowired
  private InterestRepository interestRepository;

  @Nested
  @DisplayName("좋아요 알림 등록 테스트")
  class CommentLikeTest {

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

  @Nested
  @DisplayName("관심사 알림 등록 테스트")
  class InterestNotificationTest {

    private User subscriber1;
    private User subscriber2;
    private User subscriber3;
    private Interest interest1;
    private Interest interest2;

    @BeforeEach
    void setUp() {
      notificationRepository.deleteAll();
      interestRepository.deleteAll();
      userRepository.deleteAll();

      subscriber1 = User.create("sub1@test.com", "sub1", "pwd");
      subscriber2 = User.create("sub2@test.com", "sub2", "pwd");
      subscriber3 = User.create("sub3@test.com", "sub3", "pwd");
      interest1 = Interest.create("interest1");
      interest2 = Interest.create("interest2");

      userRepository.saveAll(List.of(subscriber1, subscriber2, subscriber3));
      interestRepository.saveAll(List.of(interest1, interest2));
    }

    @Test
    @DisplayName("관심사 알림 등록 요청 시, 알림이 저장된다.")
    void shouldRegisterNotification_whenRequestInterestNotification() {
      // given
      InterestNotificationRequest request1 = new InterestNotificationRequest(interest1.getId(),
          interest1.getName(), 10,
          List.of(subscriber1.getId(), subscriber2.getId(), subscriber3.getId()));

      InterestNotificationRequest request2 = new InterestNotificationRequest(interest2.getId(),
          interest2.getName(), 3, List.of(subscriber1.getId(), subscriber2.getId()));

      // when
      notificationService.registerInterestNotification(List.of(request1, request2));

      // then
      List<Notification> results = notificationRepository.findAll();
      assertAll(
          // 1. 총 생성 개수 확인 1번 관심사 3명, 2번 관심사 2명
          () -> assertThat(results).hasSize(5),

          // 2. 리소스 타입 확인
          () -> assertThat(results).extracting(Notification::getResourceType)
              .containsOnly(NotificationResourceType.INTEREST),

          // 3-1. 관심사1의 알림 개수 및 내용 확인
          () -> {
            List<Notification> int1Notifications = results.stream()
                .filter(n -> n.getResourceId().equals(interest1.getId()))
                .toList();
            assertThat(int1Notifications).hasSize(3);
            assertThat(int1Notifications).extracting(Notification::getContent)
                .containsOnly(interest1.getName() + "와 관련된 기사가 10건 등록되었습니다.");
          },

          // 3-2. 관심사2의 알림 개수 및 내용 확인
          () -> {
            List<Notification> int2Notifications = results.stream()
                .filter(n -> n.getResourceId().equals(interest2.getId()))
                .toList();
            assertThat(int2Notifications).hasSize(2);
            assertThat(int2Notifications).extracting(Notification::getContent)
                .containsOnly(interest2.getName() + "와 관련된 기사가 3건 등록되었습니다.");
          },

          // 4. 1번 유저 2개 알림 등록
          () -> {
            List<Notification> sub1Notifications = results.stream()
                .filter(n -> n.getUser().getId().equals(subscriber1.getId()))
                .toList();
            assertThat(sub1Notifications).hasSize(2);
          },

          // 5. 확인안함 상태
          () -> assertThat(results).allSatisfy(n -> {
            assertThat(n.isConfirmed()).isFalse();
            assertThat(n.getConfirmedAt()).isNull();
          })
      );
    }
  }
}

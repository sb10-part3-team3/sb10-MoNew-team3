package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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

  @Autowired
  private MockMvc mockMvc;

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

  @Nested
  @DisplayName("알림 API 테스트")
  @Transactional
  class NotificationControllerTest {

    private User subscriber;
    private User actor;
    private Comment comment;
    private NewsArticle article;

    private Interest interest1;
    private Interest interest2;

    private Notification notification1;
    private Notification notification2;
    private Notification notification3;
    private Notification notification4;

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

    @BeforeEach
    void setUp() {
      notificationRepository.deleteAll();
      commentRepository.deleteAll();
      newsArticleRepository.deleteAll();
      newsSourceRepository.deleteAll();
      interestRepository.deleteAll();
      userRepository.deleteAll();

      subscriber = userRepository.save(User.create("subscriber0@test.com", "subscriber", "pwd"));
      actor = userRepository.save(User.create("actor@test.com", "actor", "pwd"));
      NewsSource newsSource = NewsSource.create("test", NewsSourceType.NAVER, "url");
      newsSourceRepository.save(newsSource);
      article = newsArticleRepository.save(
          NewsArticle.create(newsSource, "link", "title", Instant.now(), "summary"));
      comment = commentRepository.save(Comment.create(article, subscriber, "댓글 내용"));

      interest1 = Interest.create("interest1");
      interest2 = Interest.create("interest2");
      interestRepository.saveAll(List.of(interest1, interest2));

      notification1 = Notification.create(subscriber, "content1", NotificationResourceType.COMMENT,
          comment.getId(), actor);
      notification2 = Notification.create(subscriber, "content2", NotificationResourceType.INTEREST,
          interest1.getId(), null);
      notification3 = Notification.create(subscriber, "content3", NotificationResourceType.INTEREST,
          interest2.getId(), null);
      notification4 = Notification.create(subscriber, "content4", NotificationResourceType.COMMENT,
          comment.getId(), actor);//이미 확인된
      ReflectionTestUtils.setField(notification4, "isConfirmed", true);
      notificationRepository.saveAll(
          List.of(notification1, notification2, notification3, notification4));
    }

    @Test
    @DisplayName("모든 파라미터 없이 알림 목록을 조회한다.")
    void shouldFindAllNotificationsNotConfirmed_whenAnythingIsGiven() throws Exception {
      mockMvc.perform(get("/api/notifications")
              .header("Monew-Request-User-ID", subscriber.getId())
              .accept(MediaType.APPLICATION_JSON))
          .andExpectAll(
              status().isOk(),
              jsonPath("$.content.size()").value(3),
              jsonPath("$.content[0].resourceId").value(interest2.getId().toString()),//저장 역순
              jsonPath("$.content[1].resourceId").value(interest1.getId().toString()),
              jsonPath("$.content[2].resourceId").value(comment.getId().toString()),
              jsonPath("$.size").value(50),//기본 요청 리미트
              jsonPath("$.totalElements").value(3),//확인안된 알림 3개
              jsonPath("$.hasNext").value(false)
          );
    }

    @Test
    @DisplayName("커서 조건으로 알림 목록을 조회한다.")
    void shouldFindAllNotificationsNotConfirmed_whenCursorConditionsAreGiven() throws Exception {
      //가장 나중에 저장된 notification3을 보고 이후 목록을 요청(확인 안함 기준)
      mockMvc.perform(get("/api/notifications")
              .header("Monew-Request-User-ID", subscriber.getId())
              .param("cursor", notification3.getId().toString())
              .param("after", formatter.format(notification3.getCreatedAt()))
              .param("limit", "5")
              .accept(MediaType.APPLICATION_JSON))
          .andExpectAll(
              status().isOk(),
              jsonPath("$.content.size()").value(2),
              jsonPath("$.content[0].resourceId").value(interest1.getId().toString()),
              jsonPath("$.content[1].resourceId").value(comment.getId().toString()),
              jsonPath("$.size").value(5),//요청 리미트
              jsonPath("$.totalElements").value(3),//전체 미확인 알림 개수는 3개
              jsonPath("$.hasNext").value(false)
          );
    }

    @Test
    @DisplayName("커서 조건으로 다음 페이지가 있는 알림 목록을 조회한다.")
    void shouldFindAllNotificationsNotConfirmed_whenCursorConditionsAreGivenWithHasNext()
        throws Exception {
      //가장 나중에 저장된 notification3을 보고 이후 목록을 요청(확인 안함 기준)
      mockMvc.perform(get("/api/notifications")
              .header("Monew-Request-User-ID", subscriber.getId())
              .param("cursor", notification3.getId().toString())
              .param("after", formatter.format(notification3.getCreatedAt()))
              .param("limit", "1")
              .accept(MediaType.APPLICATION_JSON))
          .andExpectAll(
              status().isOk(),
              jsonPath("$.content.size()").value(1),
              jsonPath("$.content[0].resourceId").value(interest1.getId().toString()),
              jsonPath("$.size").value(1),//요청 리미트
              jsonPath("$.totalElements").value(3),//전체 미확인 알림 개수는 3개
              jsonPath("$.hasNext").value(true),
              jsonPath("$.nextCursor").value(notification2.getId().toString()),//마지막 확인한 요소의 값
              jsonPath("$.nextAfter").value(formatter.format(notification2.getCreatedAt()))
          );
    }

  }
}

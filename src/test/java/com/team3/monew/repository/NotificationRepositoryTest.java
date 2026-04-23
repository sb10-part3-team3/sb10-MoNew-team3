package com.team3.monew.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@EnableJpaAuditing
@ActiveProfiles("test")
class NotificationRepositoryTest {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserRepository userRepository;

  private User user1;
  private User user2;
  private User user3;
  private Notification likeNotification1;
  private Notification likeNotification2;
  private Notification confirmedNotification;
  private Notification interestNotification1;
  private Notification interestNotification2;

  @BeforeEach
  void setUp() {
    notificationRepository.deleteAll();
    userRepository.deleteAll();

    user1 = User.create("test1@test.com", "test1", "pwd");
    user2 = User.create("test2@test.com", "test2", "pwd");
    user3 = User.create("test3@test.com", "test3", "pwd");
    userRepository.saveAll(List.of(user1, user2, user3));

    UUID commentId1 = UUID.randomUUID();
    likeNotification1 = Notification.create(user1, "test2님이 당신의 댓글을 좋아합니다.",
        NotificationResourceType.COMMENT, commentId1, user2);
    likeNotification2 = Notification.create(user1, "test3님이 당신의 댓글을 좋아합니다.",
        NotificationResourceType.COMMENT, commentId1, user3);
    confirmedNotification = Notification.create(user1, "test3님이 당신의 댓글을 좋아합니다.",
        NotificationResourceType.COMMENT, commentId1, user3);

    UUID interestId1 = UUID.randomUUID();
    UUID interestId2 = UUID.randomUUID();

    interestNotification1 = Notification.create(user1,
        "interest1에 관한 기사가 10건 등록되었습니다.",
        NotificationResourceType.INTEREST,
        interestId1, null);
    interestNotification2 = Notification.create(user1, "interest2에 관한 기사가 3건 등록되었습니다.",
        NotificationResourceType.INTEREST,
        interestId2, null);

    notificationRepository.saveAll(
        List.of(likeNotification1, likeNotification2, interestNotification1,
            interestNotification2, confirmedNotification)); //5개 (4개 미확인)

    ReflectionTestUtils.setField(confirmedNotification, "isConfirmed", true);

    notificationRepository.flush();
  }

  @Test
  @DisplayName("커서와 보조 커서 없이 시간 + 아이디로 정렬된 알림 목록을 가져온다.")
  void shouldFindAllNotConfirmedNotificationByUserId_whenAnythingIsGiven() {
    //given
    UUID cursor = null;
    Instant after = null;
    Integer limit = 3;

    Pageable pageable = PageRequest.of(0, limit, Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.asc("id")
    ));

    //when
    Page<Notification> result = notificationRepository.findAllNotConfirmedNotificationByUserId(
        user1.getId(), cursor, after, pageable);

    //then
    assertNotNull(result);
    assertAll(
        // 1. 컨텐츠 검증
        () -> assertTrue(result.hasContent()),
        () -> assertThat(result.getContent().size()).isEqualTo(3),
        () -> assertThat(result.getContent().get(0).getId()).isEqualTo(
            interestNotification2.getId()),//나중에 저장(최신)
        () -> assertThat(result.getContent().get(1).getId()).isEqualTo(
            interestNotification1.getId()),
        () -> assertThat(result.getContent().get(2).getId()).isEqualTo(likeNotification2.getId()),

        // 2. 페이징 정보 검증
        () -> assertThat(result.getTotalElements()).isEqualTo(4),//전체 미확인 개수
        () -> assertThat(result.hasNext()).isTrue()

    );
  }

  @Test
  @DisplayName("커서와 보조 커서로 아이디로 정렬된 알림 목록을 가져온다.")
  void shouldFindAllNotConfirmedNotificationByUserId_whenCursorConditionsIsGiven() {
    //given
    UUID cursor = likeNotification2.getId();
    Instant after = likeNotification2.getCreatedAt();
    Integer limit = 3;

    Pageable pageable = PageRequest.of(0, limit, Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.asc("id")
    ));

    //when
    Page<Notification> result = notificationRepository.findAllNotConfirmedNotificationByUserId(
        user1.getId(), cursor, after, pageable);

    //then
    assertNotNull(result);
    assertAll(
        // 1. 컨텐츠 검증
        () -> assertTrue(result.hasContent()),
        () -> assertThat(result.getContent().size()).isEqualTo(1),
        () -> assertThat(result.getContent().get(0).getId()).isEqualTo(
            likeNotification1.getId()),//가장 먼저 저장된

        // 2. 페이징 정보 검증
        () -> assertThat(result.getTotalElements()).isEqualTo(1),//전체 미확인 개수는 별도 쿼리 필요
        () -> assertThat(result.hasNext()).isFalse()

    );
  }
}
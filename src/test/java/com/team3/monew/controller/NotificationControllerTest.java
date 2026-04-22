package com.team3.monew.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.monew.dto.notification.CursorPageResponseNotificationDto;
import com.team3.monew.dto.notification.CursorPageResponseNotificationDto.NotificationDto;
import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.repository.UserRepository;
import com.team3.monew.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@ActiveProfiles("test")
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  NotificationService notificationService;


  private static final String NOTIFICATION_URL = "/api/notifications";

  private UUID userId1;
  private UUID userId2;
  private UUID userId3;
  private User user1;
  private User user2;
  private User user3;
  private UUID commentId1;
  private UUID articleId1;

  private UUID likeNotiId1;
  private UUID likeNotiId2;
  private Notification likeNotification1;
  private Notification likeNotification2;

  private UUID interestNotiId1;
  private UUID interestNotiId2;
  private Notification interestNotification1;
  private Notification interestNotification2;
  private NotificationDto notificationDto1;
  private NotificationDto notificationDto2;
  private NotificationDto notificationDto3;
  private NotificationDto notificationDto4;

  @BeforeEach
  void setUp() {
    userId1 = UUID.randomUUID();
    userId2 = UUID.randomUUID();
    userId3 = UUID.randomUUID();
    user1 = User.create("test1@test.com", "test1", "pwd");
    user2 = User.create("test2@test.com", "test2", "pwd");
    user3 = User.create("test3@test.com", "test3", "pwd");

    ReflectionTestUtils.setField(user1, "id", userId1);
    ReflectionTestUtils.setField(user2, "id", userId2);
    ReflectionTestUtils.setField(user3, "id", userId3);

    commentId1 = UUID.randomUUID();
    articleId1 = UUID.randomUUID();

    likeNotiId1 = UUID.randomUUID();
    likeNotiId2 = UUID.randomUUID();
    likeNotification1 = Notification.create(user1, "test2님이 당신의 댓글을 좋아합니다.",
        NotificationResourceType.COMMENT, commentId1, user2);
    likeNotification2 = Notification.create(user1, "test3님이 당신의 댓글을 좋아합니다.",
        NotificationResourceType.COMMENT, commentId1, user3);
    ReflectionTestUtils.setField(likeNotification1, "id", likeNotiId1);
    ReflectionTestUtils.setField(likeNotification2, "id", likeNotiId2);
    ReflectionTestUtils.setField(likeNotification1, "createdAt",
        Instant.parse("2026-01-01T10:00:00Z"));
    ReflectionTestUtils.setField(likeNotification2, "createdAt",
        Instant.parse("2026-01-01T11:00:00Z"));

    interestNotiId1 = UUID.randomUUID();
    interestNotiId2 = UUID.randomUUID();
    interestNotification1 = Notification.create(user1, "interest1에 관한 기사가 10건 등록되었습니다.",
        NotificationResourceType.INTEREST,
        interestNotiId1, null);
    interestNotification2 = Notification.create(user1, "interest2에 관한 기사가 3건 등록되었습니다.",
        NotificationResourceType.INTEREST,
        interestNotiId2, null);
    ReflectionTestUtils.setField(interestNotification1, "id", interestNotiId1);
    ReflectionTestUtils.setField(interestNotification2, "id", interestNotiId2);
    ReflectionTestUtils.setField(interestNotification1, "createdAt",
        Instant.parse("2026-01-01T12:00:00Z"));//두개가 동일한 시간에 등록
    ReflectionTestUtils.setField(interestNotification2, "createdAt",
        Instant.parse("2026-01-01T12:00:00Z"));

    notificationDto1 = new NotificationDto(likeNotiId1, likeNotification1.getCreatedAt(),
        Instant.now(), false, likeNotification1.getUser().getId(), likeNotification1.getContent(),
        likeNotification1.getResourceType(), likeNotification1.getResourceId());
    notificationDto2 = new NotificationDto(likeNotiId2, likeNotification2.getCreatedAt(),
        Instant.now(), false, likeNotification2.getUser().getId(), likeNotification2.getContent(),
        likeNotification2.getResourceType(), likeNotification2.getResourceId());
    notificationDto3 = new NotificationDto(interestNotiId1, interestNotification1.getCreatedAt(),
        Instant.now(), false, interestNotification1.getUser().getId(),
        interestNotification1.getContent(), interestNotification1.getResourceType(),
        interestNotification1.getResourceId());
    notificationDto4 = new NotificationDto(interestNotiId2, interestNotification2.getCreatedAt(),
        Instant.now(), false, interestNotification2.getUser().getId(),
        interestNotification2.getContent(), interestNotification2.getResourceType(),
        interestNotification2.getResourceId());

  }

  @Test
  @DisplayName("커서와 보조커서, 크기로 확인 되지 않는 알림 목록을 조회할 수 있다.")
  void shouldFindAllNotConfirmed_whenCursorAndAfterAndLimitIsGiven() throws Exception {

    Instant cursor = Instant.now();
    UUID after = UUID.randomUUID();

    CursorPageResponseNotificationDto pageDto = new CursorPageResponseNotificationDto(
        List.of(notificationDto1, notificationDto2, notificationDto3), notificationDto3.createdAt(),
        notificationDto3.id(), 3, 4, true);

    given(notificationService.findAllNotConfirmed(eq(userId1), eq(cursor), eq(after),
        eq(3))).willReturn(pageDto);

    // when & then
    mockMvc.perform(get(NOTIFICATION_URL)
            .header("Monew-Request-User-ID", userId1)
            .param("cursor", cursor.toString())
            .param("after", after.toString())
            .param("limit", "3")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.size()").value(3))
        .andExpect(jsonPath("$.content[0].id").value(notificationDto1.id().toString()))
        .andExpect(jsonPath("$.nextCursor").value(notificationDto3.createdAt().toString()))
        .andExpect(jsonPath("$.nextAfter").value(notificationDto3.id().toString()))
        .andExpect(jsonPath("$.size").value(3))
        .andExpect(jsonPath("$.totalElements").value(4))
        .andExpect(jsonPath("$.hasNext").value(true));
  }

  @Test
  @DisplayName("아무 쿼리 파라미터도 없을 때 리미트 디폴트 설정으로 확인 되지 않는 알림 목록을 조회할 수 있다.")
  void shouldFindAllNotConfirmed_whenAnythingIsGiven() throws Exception {
    //given
    CursorPageResponseNotificationDto pageDto = new CursorPageResponseNotificationDto(
        List.of(notificationDto1, notificationDto2, notificationDto3, notificationDto4), null, null,
        4, 4, false);

    given(notificationService.findAllNotConfirmed(eq(userId1), eq(null), eq(null),
        eq(50))).willReturn(pageDto);

    // when & then
    mockMvc.perform(get(NOTIFICATION_URL)
            .header("Monew-Request-User-ID", userId1)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.size()").value(4))
        .andExpect(jsonPath("$.content[0].id").value(notificationDto1.id().toString()))
        .andExpect(jsonPath("$.nextCursor").value(nullValue()))
        .andExpect(jsonPath("$.nextAfter").value(nullValue()))
        .andExpect(jsonPath("$.size").value(4))
        .andExpect(jsonPath("$.totalElements").value(4))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("사용자 아이디 헤더가 포함되지 않으면 알림 목록 조회에 실패하고 예외 응답을 반환한다.)")
  void shouldFailToFindAllNotConfirmed_whenUserIdIsNotGiven() throws Exception {
    // when & then
    mockMvc.perform(get(NOTIFICATION_URL)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.details.header").value("Monew-Request-User-ID"))
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }
}
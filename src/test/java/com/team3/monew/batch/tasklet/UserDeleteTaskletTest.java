package com.team3.monew.batch.tasklet;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.CommentLikeRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.SubscriptionRepository;
import com.team3.monew.repository.UserActivityRepository;
import com.team3.monew.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.domain.PageRequest;

class UserDeleteTaskletTest {

  @InjectMocks
  private UserDeleteTasklet tasklet;

  @Mock
  private UserRepository userRepository;
  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private CommentLikeRepository commentLikeRepository;
  @Mock
  private CommentRepository commentRepository;
  @Mock
  private SubscriptionRepository subscriptionRepository;
  @Mock
  private UserActivityRepository userActivityRepository;
  @Mock
  private ArticleViewRepository articleViewRepository;

  private final Instant targetDate = Instant.now();
  private final int batchSize = 100;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("삭제 대상 유저가 있으면 모든 연관 데이터 삭제 후 CONTINUABLE 반환")
  void execute_WithUsers() {
    // given
    List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());

    given(userRepository.findDeletableUserIds(any(), any(PageRequest.class)))
        .willReturn(userIds);

    given(userRepository.deleteByIds(userIds))
        .willReturn(2);

    // when
    RepeatStatus status = tasklet.execute(targetDate, batchSize);

    // then
    then(notificationRepository).should().deleteByUserIds(userIds);
    then(commentLikeRepository).should().deleteByUserIds(userIds);
    then(commentRepository).should().deleteByUserIds(userIds);
    then(subscriptionRepository).should().deleteByUserIds(userIds);
    then(articleViewRepository).should().deleteByUserIds(userIds);

    then(userActivityRepository).should().deleteByIdIn(userIds);
    then(userActivityRepository).should().removeEmbeddedCommentsByUserIds(userIds);

    then(userRepository).should().deleteByIds(userIds);

    assertThat(status).isEqualTo(RepeatStatus.CONTINUABLE);
  }

  @Test
  @DisplayName("삭제 대상 유저가 없으면 FINISHED 반환")
  void execute_NoUsers() {
    // given
    given(userRepository.findDeletableUserIds(any(), any()))
        .willReturn(List.of());

    // when
    RepeatStatus status = tasklet.execute(targetDate, batchSize);

    // then
    then(notificationRepository).shouldHaveNoInteractions();
    then(commentLikeRepository).shouldHaveNoInteractions();
    then(commentRepository).shouldHaveNoInteractions();
    then(subscriptionRepository).shouldHaveNoInteractions();
    then(userActivityRepository).shouldHaveNoInteractions();
    then(articleViewRepository).shouldHaveNoInteractions();

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
  }

  @Test
  @DisplayName("삭제 결과가 0이면 FINISHED 반환")
  void execute_DeleteZero() {
    // given
    List<UUID> userIds = List.of(UUID.randomUUID());

    given(userRepository.findDeletableUserIds(any(), any()))
        .willReturn(userIds);

    given(userRepository.deleteByIds(userIds))
        .willReturn(0);

    // when
    RepeatStatus status = tasklet.execute(targetDate, batchSize);

    // then
    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
  }
}

package com.team3.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.monew.entity.Notification;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.NotificationResourceType;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.UserRepository;
import com.team3.monew.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBatchTest
@SpringBootTest
@Sql(scripts = "classpath:org/springframework/batch/core/schema-postgresql.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
public class NotificationDeleteBatchIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private Job notificationDeleteBatchJob;

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserRepository userRepository;

  private User user;

  private Notification notification1;
  private Notification notification2;
  private Notification notification3;
  private Notification notification4;

  @Value("${batch.notification.delete.retention-days:7}")
  private int retentionDays;

  @BeforeEach
  void setUp() {
    notificationRepository.deleteAll();
    userRepository.deleteAll();

    user = userRepository.save(User.create("subscriber0@test.com", "subscriber", "pwd"));

    notification1 = Notification.create(user, "content1", NotificationResourceType.INTEREST,
        UUID.randomUUID(), null);
    notification2 = Notification.create(user, "content2", NotificationResourceType.INTEREST,
        UUID.randomUUID(), null);
    notification3 = Notification.create(user, "content3", NotificationResourceType.INTEREST,
        UUID.randomUUID(), null);
    notification4 = Notification.create(user, "content4", NotificationResourceType.INTEREST,
        UUID.randomUUID(), null);

    int oldDay = retentionDays + 3;
    int resentDay = retentionDays - 1;
    Instant oldDate = Instant.now().minus(oldDay, ChronoUnit.DAYS); // 삭제 날보다 더 오래 전
    Instant recentDate = Instant.now().minus(resentDay, ChronoUnit.DAYS); // 삭제 기준 일보다 최신

    //예전에 확인2, 최근 확인1, 미확인1
    ReflectionTestUtils.setField(notification1, "isConfirmed", true);
    ReflectionTestUtils.setField(notification1, "confirmedAt", oldDate);
    ReflectionTestUtils.setField(notification2, "isConfirmed", true);
    ReflectionTestUtils.setField(notification2, "confirmedAt", oldDate);
    ReflectionTestUtils.setField(notification3, "isConfirmed", true);
    ReflectionTestUtils.setField(notification3, "confirmedAt", recentDate);
    ReflectionTestUtils.setField(notification4, "isConfirmed", false);
    ReflectionTestUtils.setField(notification4, "confirmedAt", null);

    notificationRepository.saveAll(
        List.of(notification1, notification2, notification3, notification4));
  }

  @Test
  @DisplayName("기준 삭제일이 지난 확인된 알림들만 삭제한다.")
  void shouldDeleteNotifications() throws Exception {
    // given
    jobLauncherTestUtils.setJob(notificationDeleteBatchJob);

    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob();

    // then
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // 최근 확인 1개, 미확인 1개 = 2개
    List<Notification> remainings = notificationRepository.findAll();
    assertThat(remainings).hasSize(2);

    List<UUID> remainingIds = remainings.stream()
        .map(Notification::getId)
        .toList();

    assertThat(remainingIds)
        .containsExactlyInAnyOrder(notification3.getId(), notification4.getId())
        .doesNotContain(notification1.getId(), notification2.getId());
  }

}

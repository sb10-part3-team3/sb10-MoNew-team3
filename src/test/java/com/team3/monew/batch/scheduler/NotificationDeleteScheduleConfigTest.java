package com.team3.monew.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
public class NotificationDeleteScheduleConfigTest {

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private Job notificationDeleteBatchJob;

  @InjectMocks
  private NotificationDeleteScheduleConfig scheduleConfig;

  @Test
  @DisplayName("배치 실행 중 예외가 발생해도 스케줄러가 중단되지 않고 예외를 잡는다.")
  void runDeleteNotifications_ShouldCatchException() throws Exception {
    // given
    given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
        .willThrow(new RuntimeException("배치 작업 실패"));

    // when & then
    //예외 전파하지 않고 로그로 남김
    assertDoesNotThrow(() -> scheduleConfig.runDeleteNotifications());
    then(jobLauncher).should(times(1))
        .run(eq(notificationDeleteBatchJob), any(JobParameters.class));
  }

  @Test
  @DisplayName("배치 상태가 FAILED일 경우에도 스케줄러가 정상적으로 실행을 완료한다.")
  void runDeleteNotifications_WhenStatusNotCompleted() throws Exception {
    // given
    JobExecution failedExecution = new JobExecution(1L);
    failedExecution.setStatus(BatchStatus.FAILED);

    given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
        .willReturn(failedExecution);

    // when
    scheduleConfig.runDeleteNotifications();

    // then
    then(jobLauncher).should().run(eq(notificationDeleteBatchJob), any(JobParameters.class));
  }
}
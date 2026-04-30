package com.team3.monew.batch.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
class UserDeleteSchedulerTest {

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private Job userDeleteJob;

  @InjectMocks
  private UserDeleteScheduler scheduler;

  @Test
  @DisplayName("배치 실행 중 예외가 발생해도 스케줄러는 예외를 잡고 종료한다")
  void run_ShouldCatchException() throws Exception {
    // given
    given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
        .willThrow(new RuntimeException("배치 실패"));

    // when & then
    assertDoesNotThrow(() -> scheduler.run());

    then(jobLauncher).should(times(1))
        .run(eq(userDeleteJob), any(JobParameters.class));
  }

  @Test
  @DisplayName("배치 상태가 FAILED여도 스케줄러는 정상 종료된다")
  void run_WhenStatusFailed() throws Exception {
    // given
    JobExecution execution = new JobExecution(1L);
    execution.setStatus(BatchStatus.FAILED);

    given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
        .willReturn(execution);

    // when
    scheduler.run();

    // then
    then(jobLauncher).should(times(1))
        .run(eq(userDeleteJob), any(JobParameters.class));
  }

  @Test
  @DisplayName("배치 상태가 COMPLETED면 정상 완료 로그 흐름")
  void run_WhenStatusCompleted() throws Exception {
    // given
    JobExecution execution = new JobExecution(1L);
    execution.setStatus(BatchStatus.COMPLETED);

    given(jobLauncher.run(any(Job.class), any(JobParameters.class)))
        .willReturn(execution);

    // when
    scheduler.run();

    // then
    then(jobLauncher).should(times(1))
        .run(eq(userDeleteJob), any(JobParameters.class));
  }
}

package com.team3.monew.batch.scheduler;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserDeleteScheduler {

  private final JobLauncher jobLauncher;
  private final Job userDeleteJob;

  @Scheduled(cron = "${batch.user.delete.cron:0 0 2 * * *}")
  public void run() {
    try {
      log.info("User 삭제 배치 시작");

      JobParameters params = new JobParametersBuilder()
          .addLocalDateTime("runTime", LocalDateTime.now())
          .toJobParameters();

      JobExecution execution = jobLauncher.run(userDeleteJob, params);

      if (execution.getStatus() == BatchStatus.COMPLETED) {
        log.info("User 삭제 배치 완료");
      } else {
        log.error("User 삭제 배치 실패: {}", execution.getStatus());
      }

    } catch (Exception e) {
      log.error("User 삭제 배치 실행 실패", e);
    }
  }
}

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
public class NotificationDeleteScheduleConfig {

  private final JobLauncher jobLauncher;
  private final Job notificationDeleteBatchJob;// Bean 이름으로 자동 주입

  @Scheduled(cron = "${batch.notification.delete.cron:0 0 3 * * *}")
  public void runDeleteNotifications() {
    try {
      log.info("알림 삭제 배치 스케줄러 시작");
      JobParameters jobParameters = new JobParametersBuilder()
          .addLocalDateTime("runTime", LocalDateTime.now())
          .toJobParameters();
      JobExecution execution = jobLauncher.run(notificationDeleteBatchJob, jobParameters);

      if (execution.getStatus() == BatchStatus.COMPLETED) {
        log.info("알림 삭제 배치 스케줄러 완료");
      } else {
        log.error("알림 삭제 배치 스케줄러 오류 발생: 샹태={}", execution.getStatus());
      }
    } catch (Exception e) {
      log.error("알림 삭제 스케줄러 실행 실패", e);
    }
  }
}

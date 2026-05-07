package com.team3.monew.batch.scheduler;

import com.team3.monew.monitoring.BatchMetrics;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "app.news", name = "collector-mode", havingValue = "batch")
@RequiredArgsConstructor
public class ArticleCollectBatchScheduleConfig {

  private final JobLauncher jobLauncher;
  private final BatchMetrics batchMetrics;
  private final Job articleCollectBatchJob;

  @Scheduled(cron = "${app.cron.news-collections:0 0/5 * * * *}")
  public void runArticleCollect() {
    long startTime = System.currentTimeMillis();
    try {
      log.info("뉴스기사 수집 배치 스케줄러 시작");
      ZoneId zone = ZoneId.of("Asia/Seoul");
      JobParameters jobParameters = new JobParametersBuilder()
          .addLocalDateTime("collectedAt", LocalDateTime.now(zone))
          .toJobParameters();
      JobExecution execution = jobLauncher.run(articleCollectBatchJob, jobParameters);

      if (execution.getStatus() == BatchStatus.COMPLETED) {
        batchMetrics.recordNewsCollectSuccess(System.currentTimeMillis() - startTime,
            execution.getExecutionContext().getLong("savedArticleCount", 0L));
        log.info("뉴스기사 수집 배치 스케줄러 완료");
      } else {
        batchMetrics.recordNewsCollectFailure(System.currentTimeMillis() - startTime);
        log.error("뉴스기사 수집 배치 스케줄러 오류 발생 - status={}", execution.getStatus());
      }
    } catch (Exception e) {
      batchMetrics.recordNewsCollectFailure(System.currentTimeMillis() - startTime);
      log.error("뉴스기사 수집 배치 스케줄러 실행 실패", e);
    }
  }
}

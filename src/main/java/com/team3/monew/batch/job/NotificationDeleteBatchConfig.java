package com.team3.monew.batch.job;

import com.team3.monew.entity.Notification;
import com.team3.monew.repository.NotificationRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotificationDeleteBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager platformTransactionManager;
  private final NotificationRepository notificationRepository;

  @Value("${batch.notification.delete.batch-size:100}")
  private int batchSize;

  @Value("${batch.notification.delete.retention-days:7}")
  private int retentionDays;

  @Bean
  public Job notificationDeleteBatchJob() {
    return new JobBuilder("notificationDeleteBatchJob", jobRepository)
        .start(deleteNotificationsStep(null))
        .build();
  }

  @Bean
  @JobScope
  public Step deleteNotificationsStep(
      @Value("#{jobParameters['runTime']}") LocalDateTime runTime) {
    // 타겟 날짜 고정
    Instant targetDate = (runTime != null ? runTime : LocalDateTime.now())
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .minus(retentionDays, ChronoUnit.DAYS);

    return new StepBuilder("deleteNotificationsStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {

          int deletedCount = notificationRepository.deleteOldConfirmedNotifications(targetDate,
              batchSize);
          log.info("알림 배치 삭제: {}건 삭제", deletedCount);

          if (deletedCount > 0) {//0건 삭제 시까지 반복 삭제
            return RepeatStatus.CONTINUABLE;
          } else {
            return RepeatStatus.FINISHED;
          }
        }, platformTransactionManager)
        .build();
  }

}

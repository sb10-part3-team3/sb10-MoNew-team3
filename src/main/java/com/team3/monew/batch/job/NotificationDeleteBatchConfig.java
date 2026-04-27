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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
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
  private final EntityManagerFactory entityManagerFactory;

  @Value("${batch.notification.delete.chunk-size:100}")
  private int chunkSize;

  @Value("${batch.notification.delete.retention-days:7}")
  private int retentionDays;

  @Bean
  public Job notificationDeleteBatchJob() {
    return new JobBuilder("notificationDeleteBatchJob", jobRepository)
        .start(deleteNotificationsStep())
        .build();
  }

  @Bean
  public Step deleteNotificationsStep() {
    return new StepBuilder("deleteNotificationsStep", jobRepository)
        .<Notification, Notification>chunk(chunkSize, platformTransactionManager)
        .reader(confirmedNotificationReader(null))
        .writer(confirmedNotificationWriter())
        .build();
  }

  // 읽기
  @Bean
  @StepScope //날짜 재설정
  public JpaCursorItemReader<Notification> confirmedNotificationReader(
      @Value("#{jobParameters['runTime']}") LocalDateTime runTime) {
    // 직접 날짜를 지정하여 삭제할 수도 있기때문에
    LocalDateTime baseTime = (runTime != null) ? runTime : LocalDateTime.now();

    Instant targetDate = baseTime.atZone(ZoneId.systemDefault())
        .toInstant()
        .minus(retentionDays, ChronoUnit.DAYS);

    return new JpaCursorItemReaderBuilder<Notification>()
        .name("confirmedNotificationReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString(
            "select n from Notification n where n.isConfirmed = true and n.confirmedAt <= :targetDate order by n.confirmedAt asc, n.id desc")
        .parameterValues(Map.of("targetDate", targetDate)) // 값 매핑
        .build();
  }

  @Bean
  public ItemWriter<Notification> confirmedNotificationWriter() {
    return chunks -> {
      log.info("알림 삭제: 삭제 건수={}", chunks.size());
      notificationRepository.deleteAllInBatch(new ArrayList<>(chunks.getItems()));
    };
  }
}

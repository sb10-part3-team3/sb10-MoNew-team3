package com.team3.monew.batch.job;

import com.team3.monew.batch.tasklet.UserDeleteTasklet;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class UserDeleteBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final UserDeleteTasklet userDeleteTasklet;

  @Value("${batch.user.delete.batch-size:100}")
  private int batchSize;

  @Value("${batch.user.delete.retention-days:1}")
  private int retentionDays;

  @PostConstruct
  void validate() {
    if (batchSize <= 0) throw new IllegalArgumentException("배치 사이즈는 0보다 커야합니다.");
    if (retentionDays <= 0) throw new IllegalArgumentException("삭제 주기는 0보다 커야힙니다.");
  }

  @Bean
  public Job userDeleteJob() {
    return new JobBuilder("userDeleteJob", jobRepository)
        .start(deleteUserStep(null))
        .build();
  }

  @Bean
  @JobScope
  public Step deleteUserStep(
      @Value("#{jobParameters['runTime']}") LocalDateTime runTime
  ) {

    Instant targetDate = (runTime != null ? runTime : LocalDateTime.now())
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .minus(retentionDays, ChronoUnit.DAYS);

    return new StepBuilder("deleteUserStep", jobRepository)
        .tasklet((contribution, chunkContext) ->
            userDeleteTasklet.execute(targetDate, batchSize, contribution), transactionManager)
        .build();
  }
}

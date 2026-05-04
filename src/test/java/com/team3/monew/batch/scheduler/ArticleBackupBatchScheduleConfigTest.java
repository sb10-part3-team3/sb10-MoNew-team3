package com.team3.monew.batch.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

import com.team3.monew.config.AwsProperties;
import com.team3.monew.config.AwsProperties.S3;
import com.team3.monew.monitoring.BatchMetrics;
import com.team3.monew.service.ArticleBackupJobLogService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ArticleBackupBatchScheduleConfigTest {

  @Mock
  private JobLauncher jobLauncher;
  @Mock
  private AwsProperties awsProperties;
  @Mock
  private ArticleBackupJobLogService articleBackupJobLogService;
  @Mock
  private BatchMetrics batchMetrics;

  @InjectMocks
  private ArticleBackupBatchScheduleConfig articleBackupBatchScheduleConfig;

  @BeforeEach
  void setUp() {
    S3 s3 = new S3();
    s3.setBucket("bucket");
    given(awsProperties.getS3()).willReturn(s3);
  }

  @Test
  @DisplayName("배치 스케줄러 예외가 발생하면 에러 로그와 backupLog서비스를 기록한다")
  void shouldRecordLog_whenSchedulerThrowError() throws Exception {
    // given
    given(jobLauncher.run(any(), any())).willThrow(new RuntimeException("스케줄러 에러"));
    willDoNothing().given(articleBackupJobLogService).recordFailed(any(), any());

    // when
    articleBackupBatchScheduleConfig.runArticleBackup();

    // then
    then(articleBackupJobLogService).should()
        .recordFailed(any(), contains("RuntimeException"));
  }

  @Test
  @DisplayName("배치 스케줄러가 FAILED인 경우 에러 로그와 backupLog서비스를 기록한다")
  void shouldRecordLog_whenSchedulerStatusIsFailed() throws Exception {
    // given
    JobExecution execution = new JobExecution(22L);
    execution.setStatus(BatchStatus.FAILED);
    execution.addFailureException(new RuntimeException("스케줄러 내부 에러"));
    given(jobLauncher.run(any(), any())).willReturn(execution);
    willDoNothing().given(articleBackupJobLogService).recordFailed(any(), any());

    // when
    articleBackupBatchScheduleConfig.runArticleBackup();

    // then
    then(articleBackupJobLogService).should().recordFailed(any(), any());
  }

  @Test
  @DisplayName("배치 스케줄러가 COMPLETED인 경우 성공 로그와 backupLog서비스를 기록한다")
  void shouldRecordLog_whenSchedulerStatusIsCompleted() throws Exception {
    // given
    long writeCount = 500L;
    JobExecution execution = new JobExecution(22L);
    execution.setStatus(BatchStatus.COMPLETED);
    StepExecution stepExecution = new StepExecution("exportArticlesToLocalStep", execution);
    stepExecution.setWriteCount(writeCount);
    execution.addStepExecutions(List.of(stepExecution));

    given(jobLauncher.run(any(), any())).willReturn(execution);
    willDoNothing().given(articleBackupJobLogService).recordSuccess(any(), eq((int) writeCount));

    // when
    articleBackupBatchScheduleConfig.runArticleBackup();

    // then
    then(articleBackupJobLogService).should().recordSuccess(any(), eq((int) writeCount));
  }
}

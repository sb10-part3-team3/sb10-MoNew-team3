package com.team3.monew.batch.scheduler;

import com.team3.monew.config.AwsProperties;
import com.team3.monew.service.ArticleBackupJobLogService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArticleBackupBatchScheduleConfig {

  private final JobLauncher jobLauncher;
  private final Job articleBackupBatchJob;
  private final AwsProperties awsProperties;
  private final ArticleBackupJobLogService articleBackupJobLogService;

  @Value("${batch.backup.uri:test/csj}")
  private String backupUri;
  @Value("${batch.backup.zone:Asia/Seoul}")
  private String zone = "Asia/Seoul";
  private final DateTimeFormatter S3_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

  @Scheduled(cron = "${batch.backup.cron:0 5 0 * * *}", zone = "${batch.backup.zone:Asia/Seoul}")
  void runArticleBackup() {
    // 시간 설정
    ZoneId zoneId = ZoneId.of(zone);
    LocalDate today = LocalDate.now(zoneId);
    LocalDate yesterday = today.minusDays(1);

    // 파일명 & S3 Key 설정
    String tmpFileName = "tmp_" + yesterday + ".jsonl";
    String fileName = "backup_" + yesterday + ".jsonl.gz";
    String key = generateS3Key(yesterday, fileName);    // 경로/yyyy/MM/backUp_yyyy-MM-dd.jsonl.gz
    String bucket = awsProperties.getS3().getBucket();

    // backupJob 생성
    UUID backupJobId = articleBackupJobLogService.createBackupJob(yesterday, bucket, key);

    log.debug("뉴스기사 백업 배치 시작 - backupTargetDate={}", yesterday);

    try {
      JobParameters jobParameters = new JobParametersBuilder()
          .addLocalDate("today", today)
          .addLocalDate("yesterday", yesterday)
          .addString("tmpFileName", tmpFileName)
          .addString("fileName", fileName)
          .addString("bucket", bucket)
          .addString("key", key)
          .toJobParameters();
      JobExecution execution = jobLauncher.run(articleBackupBatchJob, jobParameters);

      if (execution.getStatus() == BatchStatus.COMPLETED) {
        long totalWriteCount = execution.getStepExecutions().stream()
            .filter(step -> step.getStepName().equals("exportArticlesToLocalStep"))
            .mapToLong(StepExecution::getWriteCount)
            .sum();
        log.info("뉴스기사 백업 배치 성공 - backupTargetDate={}, articleCount={}",
            yesterday, totalWriteCount);
        articleBackupJobLogService.recordSuccess(backupJobId, (int) totalWriteCount);

        Path tmpFile = Paths.get(tmpFileName);    // 압축 전 파일
        Files.deleteIfExists(tmpFile);
        Path file = Paths.get(fileName);          // 압축 후 파일
        Files.deleteIfExists(file);
      } else {
        List<Throwable> exceptions = execution.getAllFailureExceptions();
        if (!exceptions.isEmpty()) {
          log.error("뉴스기사 백업 배치 실패 - status={}, message={}",
              execution.getStatus(), exceptions.get(0).getMessage(), exceptions.get(0));
          articleBackupJobLogService.recordFailed(backupJobId, exceptions.get(0).getMessage());
        }
      }
    } catch (Exception e) {
      String errorMessage = "뉴스기사 백업 배치 스케줄러 오류 - exceptionName=" + e.getClass().getSimpleName();
      log.error(errorMessage, e);
      articleBackupJobLogService.recordFailed(backupJobId, errorMessage);
    }
  }

  private String generateS3Key(LocalDate localDate, String fileName) {
    return backupUri + "/" + localDate.format(S3_PATH_FORMATTER) + "/" + fileName;
  }
}

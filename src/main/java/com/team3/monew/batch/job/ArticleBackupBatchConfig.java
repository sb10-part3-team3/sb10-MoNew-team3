package com.team3.monew.batch.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.config.AwsProperties;
import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.NewsArticleRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.transaction.PlatformTransactionManager;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Configuration
public class ArticleBackupBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager platformTransactionManager;
  private final NewsArticleRepository newsArticleRepository;
  private final ArticleMapper articleMapper;
  private final S3Client s3Client;
  private final AwsProperties awsProperties;
  private final ObjectMapper backupObjectMapper;

  @Value("${batch.backup.chunk-size:1000}")
  private int chunkSize;
  @Value("${batch.backup.page-size:1000}")
  private int pageSize;
  @Value("${batch.backup.zone:Asia/Seoul}")
  private String zone;

  public ArticleBackupBatchConfig(JobRepository jobRepository,
      PlatformTransactionManager platformTransactionManager,
      NewsArticleRepository newsArticleRepository, ArticleMapper articleMapper, S3Client s3Client,
      AwsProperties awsProperties,
      @Qualifier("backupObjectMapper") ObjectMapper backupObjectMapper) {
    this.jobRepository = jobRepository;
    this.platformTransactionManager = platformTransactionManager;
    this.newsArticleRepository = newsArticleRepository;
    this.articleMapper = articleMapper;
    this.s3Client = s3Client;
    this.awsProperties = awsProperties;
    this.backupObjectMapper = backupObjectMapper;
  }

  @Bean
  public Job articleBackupBatchJob() {
    return new JobBuilder("articleBackupBatchJob", jobRepository)
        .start(exportArticlesToLocalStep(null))
        .next(compressBackupFileStep(null, null))
        .next(uploadBackupFileToS3Step(null, null))
        .build();
  }

  @Bean
  @JobScope
  public Step exportArticlesToLocalStep(@Value("#{jobParameters['today']}") LocalDate today) {
    return new StepBuilder("exportArticlesToLocalStep", jobRepository)
        .<NewsArticle, ArticleBackup>chunk(chunkSize, platformTransactionManager)
        .reader(articleReader(null))
        .processor(articleProcessor())
        .writer(articleWriter(null))
        .build();
  }

  @Bean
  @JobScope
  public Step compressBackupFileStep(
      @Value("#{jobParameters['tmpFileName']}") String tmpFileName,
      @Value("#{jobParameters['fileName']}") String fileName) {
    return new StepBuilder("compressBackupFileStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          File inputFile = new File(tmpFileName);
          File outputFile = new File(fileName);

          try (FileInputStream fis = new FileInputStream(inputFile);
              FileOutputStream fos = new FileOutputStream(outputFile);
              GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
              gzos.write(buffer, 0, len);
            }
          }

          return RepeatStatus.FINISHED;
        }, platformTransactionManager)
        .build();
  }

  @Bean
  @JobScope
  public Step uploadBackupFileToS3Step(
      @Value("#{jobParameters['key']}") String key,
      @Value("#{jobParameters['fileName']}") String fileName
  ) {
    return new StepBuilder("uploadBackupFileToS3Step", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          s3Client.putObject(
              req -> req.bucket(awsProperties.getS3().getBucket())
                  .key(key)
                  .contentEncoding("gzip")
                  .contentType("application/x-jsonlines"),
              RequestBody.fromFile(new File(fileName)));

          return RepeatStatus.FINISHED;

        }, platformTransactionManager).build();
  }

  @Bean
  @StepScope
  public RepositoryItemReader<NewsArticle> articleReader(
      @Value("#{jobParameters['today']}") LocalDate today) {
    Instant startOfToday = today.atStartOfDay(ZoneId.of(zone)).toInstant();
    Instant startOfYesterday = startOfToday.minus(1, ChronoUnit.DAYS);

    Map<String, Direction> sortsMap = new LinkedHashMap<>();
    sortsMap.put("publishedAt", Direction.ASC);   // 1차정렬: 발행일 오름차순
    sortsMap.put("id", Direction.ASC);            // 2차정렬: id    오름차순

    return new RepositoryItemReaderBuilder<NewsArticle>()
        .name("articleReader")
        .repository(newsArticleRepository)
        .methodName("findAllByPeriod")
        .arguments(List.of(startOfYesterday, startOfToday))
        .pageSize(pageSize)
        .sorts(sortsMap)
        .build();
  }

  @Bean
  public ItemProcessor<NewsArticle, ArticleBackup> articleProcessor() {
    return articleMapper::toBackupDto;
  }

  @Bean
  @StepScope
  public FlatFileItemWriter<ArticleBackup> articleWriter(
      @Value("#{jobParameters['tmpFileName']}") String tmpFileName) {
    return new FlatFileItemWriterBuilder<ArticleBackup>()
        .name("articleWriter")
        .resource(new FileSystemResource(tmpFileName))
        .append(true)                             // 파일 이어 저장하기
        .shouldDeleteIfExists(false)   // 재시작시 파일 지우지 않음
        .lineAggregator(item -> {
          try {
            return backupObjectMapper.writeValueAsString(item);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        })
        .build();
  }
}

package com.team3.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.monew.config.AwsProperties;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import com.team3.monew.support.IntegrationTestSupport;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@SpringBatchTest
@Tag("external-api")
@Sql(scripts = "classpath:sql/batch-schema.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@TestPropertySource(locations = "file:.env")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)   // 환경 격리용
public class ArticleBackupBatchIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired
  private Job articleBackupBatchJob;
  @Autowired
  private AwsProperties awsProperties;
  @Autowired
  private NewsSourceRepository newsSourceRepository;
  @Autowired
  private NewsArticleRepository newsArticleRepository;
  @TempDir
  private Path tempDir;

  private NewsArticle newsArticle1;
  private NewsArticle newsArticle2;
  private NewsArticle newsArticle3;
  private NewsArticle newsArticle4;
  private NewsArticle newsArticle5;

  private LocalDate today;
  private String tmpFileName;
  private String fileName;
  private String key;
  private String bucket;

  private static final String BACKUP_URI = "test/csj";
  private static final DateTimeFormatter S3_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

  @BeforeEach
  void setUp() {
    ZoneId zone = ZoneId.of("Asia/Seoul");
    today = LocalDate.now(zone);
    LocalDate yesterday = today.minusDays(1);
    Instant startOfToday = today.atStartOfDay(zone).toInstant();
    Instant startOfYesterday = startOfToday.minus(1, ChronoUnit.DAYS);

    NewsSource testNaverSource = NewsSource
        .create(NewsSourceType.NAVER.name() + 1, NewsSourceType.NAVER, "baseUrl");
    newsSourceRepository.saveAndFlush(testNaverSource);

    newsArticle1 = NewsArticle.create(
        testNaverSource, "originalLink1", "title1", startOfYesterday.minus(2, ChronoUnit.HOURS),
        "summary1");
    newsArticle2 = NewsArticle.create(
        testNaverSource, "originalLink2", "title2", startOfYesterday, "summary2");
    newsArticle3 = NewsArticle.create(
        testNaverSource, "originalLink3", "title3", startOfYesterday.plus(7, ChronoUnit.HOURS),
        "summary3");
    newsArticle4 = NewsArticle.create(
        testNaverSource, "originalLink4", "title4", startOfToday.minusMillis(1), "summary4");
    newsArticle5 = NewsArticle.create(
        testNaverSource, "originalLink5", "title5", startOfToday, "summary5");
    List<NewsArticle> articles = List.of(newsArticle1, newsArticle2, newsArticle3, newsArticle4,
        newsArticle5);
    newsArticleRepository.saveAll(articles);

    String keyFilename = "backup_" + yesterday + ".jsonl.gz";
    tmpFileName = tempDir.resolve("tmp_" + yesterday + ".jsonl").toString();
    fileName = tempDir.resolve(keyFilename).toString();
    bucket = awsProperties.getS3().getBucket();
    key = generateS3Key(yesterday, keyFilename); // 경로/yyyy/MM/backUp_yyyy-MM-dd.jsonl.gz
  }

  @Test
  @DisplayName("어제날짜의 기사들을 가져와서 압축하고 s3에 백업한다")
  void shouldBackupCompressedArticles_whenYesterdayArticlesIsGiven() throws Exception {
    // given
    jobLauncherTestUtils.setJob(articleBackupBatchJob);
    JobParameters jobParameters = new JobParametersBuilder()
        .addLocalDate("today", today)
        .addString("tmpFileName", tmpFileName)
        .addString("fileName", fileName)
        .addString("bucket", bucket)
        .addString("key", key)
        .toJobParameters();

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(jobParameters);

    // then
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    long totalWriteCount = execution.getStepExecutions().stream()
        .filter(step -> step.getStepName().equals("exportArticlesToLocalStep"))
        .mapToLong(StepExecution::getWriteCount)
        .sum();
    assertThat(totalWriteCount).isEqualTo(3);
  }

  private String generateS3Key(LocalDate localDate, String fileName) {
    return BACKUP_URI + "/" + localDate.format(S3_PATH_FORMATTER) + "/" + fileName;
  }
}

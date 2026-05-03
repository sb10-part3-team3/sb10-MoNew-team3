package com.team3.monew.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.NewsArticleRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@Tag("unit")
@ActiveProfiles("test")
@SpringBatchTest
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class ArticleBackupBatchConfigTest {


  private ZoneId zone;
  private LocalDate today;
  private LocalDate yesterday;
  private String fileName;

  @BeforeEach
  void setUp() {
    zone = ZoneId.of("Asia/Seoul");
    today = LocalDate.now(zone);
    yesterday = today.minusDays(1);

    fileName = "backup_" + yesterday + ".jsonl.gz";
  }

  @Nested
  class ArticleReader {

    @Autowired
    private RepositoryItemReader<NewsArticle> reader;

    @MockitoBean
    private NewsArticleRepository newsArticleRepository;

    public StepExecution getStepExecution() {
      zone = ZoneId.of("Asia/Seoul");
      LocalDate today = LocalDate.now(zone);
      JobParameters jobParameters = new JobParametersBuilder()
          .addLocalDate("today", today)
          .toJobParameters();
      return MetaDataInstanceFactory.createStepExecution(jobParameters);
    }

    @AfterEach
    void tearDown() {
      reader.close();
    }

    @Test
    @DisplayName("어제기간의 기사를 가져온다")
    void shouldReturnArticles_whenYesterdayIsGiven() throws Exception {
      // given
      Instant startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant();
      Instant startOfYesterday = startOfToday.minus(1, ChronoUnit.DAYS);

      NewsSource naverSource = NewsSource
          .create(NewsSourceType.NAVER.name(), NewsSourceType.NAVER, "baseUrl");
      NewsArticle newsArticle1 = NewsArticle.create(
          naverSource, "link1", "title1", startOfYesterday, "summary1");
      NewsArticle newsArticle2 = NewsArticle.create(
          naverSource, "link2", "title2", startOfYesterday.plus(7, ChronoUnit.HOURS), "summary2");
      List<NewsArticle> articles = List.of(newsArticle1, newsArticle2);
      given(newsArticleRepository.findAllByPeriod(any(), any(), any(Pageable.class)))
          .willReturn(new PageImpl<>(articles))                   // 첫 페이지
          .willReturn(new PageImpl<>(Collections.emptyList()));   // 두번째 페이지

      // when
      reader.open(new ExecutionContext());
      NewsArticle firstArticle = reader.read();
      NewsArticle secondArticle = reader.read();
      NewsArticle thirdArticle = reader.read();   // 없으면 null 반환

      // then
      assertThat(firstArticle).isNotNull()
          .extracting("title").isEqualTo("title1");
      assertThat(secondArticle).isNotNull()
          .extracting("title").isEqualTo("title2");
      assertThat(thirdArticle).isNull();
    }
  }

  @Nested
  class ArticleProcessor {

    @Mock
    private ArticleMapper articleMapper;

    @Test
    @DisplayName("NewsArticle을 ArticleBackup으로 변환한다")
    void shouldTransformToArticleBackup_whenNewsArticleIsGiven() throws Exception {
      // given
      Instant now = Instant.now();
      NewsSource naverSource = NewsSource
          .create(NewsSourceType.NAVER.name(), NewsSourceType.NAVER, "baseUrl");
      NewsArticle article = NewsArticle
          .create(naverSource, "link", "title", now, "summary");
      ArticleBackup articleBackup =
          new ArticleBackup(NewsSourceType.NAVER, "link", "title", now, "summary");
      given(articleMapper.toBackupDto(any(NewsArticle.class))).willReturn(articleBackup);

      // when
      ItemProcessor<NewsArticle, ArticleBackup> processor = item -> articleMapper.toBackupDto(item);
      ArticleBackup actual = processor.process(article);

      // then
      assertEquals(article.getOriginalLink(), actual.originalLink());
      assertEquals(article.getTitle(), actual.title());
      assertEquals(article.getPublishedAt(), actual.publishedAt());
      assertEquals(article.getSummary(), actual.summary());
    }

  }

  @Nested
  class ArticleWriter {

    private String tmpFileName;

    @MockitoSpyBean
    private ObjectMapper backupObjectMapper;
    @Autowired
    private FlatFileItemWriter<ArticleBackup> writer;

    public StepExecution getStepExecution() {
      tmpFileName = "test_" + System.currentTimeMillis() + ".jsonl";
      JobParameters jobParameters = new JobParametersBuilder()
          .addString("tmpFileName", tmpFileName)
          .toJobParameters();
      return MetaDataInstanceFactory.createStepExecution(jobParameters);
    }

    @AfterEach
    void tearDown() throws IOException {
      writer.close();
      Path path = Paths.get(tmpFileName);
      Files.deleteIfExists(path);
    }

    @Test
    @DisplayName("ArticleBackup을 jsonl로 변환해서 파일에 저장한다")
    void shouldWriteJsonlContent_whenArticleBackupIsGiven() throws Exception {
      // given
      Instant now = Instant.now();
      ArticleBackup articleBackup =
          new ArticleBackup(NewsSourceType.NAVER, "link", "title", now, "summary");

      // when
      writer.open(new ExecutionContext());
      writer.write(Chunk.of(articleBackup));

      // then
      Path path = Paths.get(tmpFileName);
      List<String> lines = Files.readAllLines(path);

      assertThat(lines).hasSize(1);
      assertThat(lines.get(0))
          .contains("\"sourceType\":\"NAVER\"")
          .contains("\"originalLink\":\"link\"")
          .contains("\"title\":\"title\"")
          .contains("\"summary\":\"summary\"")
          .contains("\"publishedAt\":\"" + now.toString() + "\"");
    }

    @Test
    @DisplayName("잘못된 객체가 들어와서 json파싱이 실패하면 Exception을 던진다")
    void shouldThrowException_whenInvalidDataIsGiven() throws Exception {
      // given
      ArticleBackup articleBackup =
          new ArticleBackup(NewsSourceType.NAVER, "link", "title", Instant.now(), "summary");
      given(backupObjectMapper.writeValueAsString(any())).willThrow(
          new JsonParseException("error"));

      // when
      writer.open(new ExecutionContext());
      assertThrows(RuntimeException.class,
          () -> writer.write(Chunk.of(articleBackup)));
    }
  }
}
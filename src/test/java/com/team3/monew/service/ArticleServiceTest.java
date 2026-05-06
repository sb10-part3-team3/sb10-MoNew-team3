package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.dto.article.ArticleDto;
import com.team3.monew.dto.article.ArticleRestoreResultDto;
import com.team3.monew.dto.article.ArticleSearchRequest;
import com.team3.monew.dto.article.internal.ArticleCursor;
import com.team3.monew.dto.article.internal.ArticleSearchCondition;
import com.team3.monew.dto.article.internal.enums.ArticleDirection;
import com.team3.monew.dto.article.internal.enums.ArticleOrderBy;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.entity.ArticleBackupJob;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.BackupJobStatus;
import com.team3.monew.entity.enums.BackupJobType;
import com.team3.monew.entity.enums.DeleteStatus;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.exception.article.ArticleInvalidPeriodException;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.ArticleBackupJobRepository;
import com.team3.monew.repository.ArticleInterestRepository;
import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsArticleRepository.ArticleCountInfo;
import com.team3.monew.repository.NewsArticleRepository.ArticleLinkAndPublishedAt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Tag("unit")
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

  @Spy
  private ArticleMapper articleMapper = Mappers.getMapper(ArticleMapper.class);
  @Mock
  private NewsArticleRepository newsArticleRepository;
  @Mock
  private ArticleViewRepository articleViewRepository;
  @Mock
  private ArticleInterestRepository articleInterestRepository;
  @Mock
  private CommentRepository commentRepository;
  @Mock
  private ArticleViewService articleViewService;
  @Mock
  private ArticleBackupJobRepository articleBackupJobRepository;
  @Mock
  private S3AsyncClient s3AsyncClient;
  @Mock
  private ArticleBackupJobLogService articleBackupJobLogService;
  @Mock
  private TaskExecutor decompressTaskExecutor;
  @Mock
  private ObjectMapper mockedBackupObjectMapper;
  @Mock
  private ArticleBatchService articleBatchService;

  @Spy
  @InjectMocks
  private ArticleService articleService;

  private NewsSource naverSource;

  private ArticleSearchRequest requestPublishDate;
  private ArticleSearchRequest requestCommentCount;
  private ArticleSearchRequest requestViewCount;

  private Instant testGetCreatedAt = Instant.now();

  @BeforeEach
  void setUp() {
    naverSource = NewsSource.create("NAVER", NewsSourceType.NAVER, "baseUrl..");

    requestPublishDate = new ArticleSearchRequest("검색", null, List.of(NewsSourceType.NAVER),
        null, null, ArticleOrderBy.PUBLISH_DATE, ArticleDirection.DESC,
        Instant.now().minus(2, ChronoUnit.DAYS).toString() + ", " + testGetCreatedAt.toString(),
        null, 2);
    requestCommentCount = new ArticleSearchRequest(null, UUID.randomUUID(),
        List.of(NewsSourceType.NAVER),
        LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(1),
        ArticleOrderBy.COMMENT_COUNT, ArticleDirection.ASC,
        "5, " + testGetCreatedAt.toString(), null, 1);
    requestViewCount = new ArticleSearchRequest(null, UUID.randomUUID(),
        List.of(NewsSourceType.NAVER),
        null, null, ArticleOrderBy.VIEW_COUNT, ArticleDirection.DESC,
        null, null, 10);
  }


  @Nested
  @DisplayName("뉴스기사 목록을 조회한다")
  class GetArticleList {

    @Test
    @DisplayName("요청dto에서 검색조건dto로 변환이 잘 되는지 검증한다")
    void shouldValidateTransferToConditionDto_whenRequestDtoIsProvided() {
      // given
      UUID interestId = UUID.randomUUID();
      LocalDateTime publishDateFrom = LocalDateTime.now().minusDays(30);
      LocalDateTime publishDateTo = LocalDateTime.now().minusDays(10);
      Instant timeCursor = Instant.now();
      Instant after = Instant.now();

      String timeCursorString = timeCursor.toString();

      ArticleSearchRequest request = new ArticleSearchRequest(
          "키워드", interestId, List.of(NewsSourceType.NAVER, NewsSourceType.CHOSUN),
          publishDateFrom, publishDateTo, ArticleOrderBy.PUBLISH_DATE, ArticleDirection.DESC,
          timeCursorString,
          after, 2);
      ArticleCursor timeArticleCursor = new ArticleCursor(timeCursor, after);

      // when
      ArticleSearchCondition cond = articleMapper.toCondition(request, timeArticleCursor);

      // then
      assertEquals(request.keyword(), cond.keyword());
      assertEquals(request.interestId(), cond.interestId());
      assertEquals(request.sourceIn(), cond.sourceIn());
      assertEquals(request.publishDateFrom().atZone(ZoneId.of("Asia/Seoul")).toInstant(),
          cond.publishDateFrom());
      assertEquals(request.publishDateTo().atZone(ZoneId.of("Asia/Seoul")).toInstant(),
          cond.publishDateTo());
      assertEquals(request.orderBy(), cond.articleOrderBy());
      assertEquals(request.direction(), cond.direction());
      assertEquals(request.limit(), cond.limit());
      assertEquals(request.after(), cond.cursor().after());
      assertEquals(request.cursor(), cond.cursor().cursor().toString());

      // given
      Integer intCursor = 25;
      ArticleSearchRequest request2 = new ArticleSearchRequest(
          "키워드", interestId, List.of(NewsSourceType.NAVER, NewsSourceType.CHOSUN),
          publishDateFrom, publishDateTo, ArticleOrderBy.PUBLISH_DATE, ArticleDirection.DESC,
          intCursor.toString(),
          after, 10);
      ArticleCursor intArticleCursor = new ArticleCursor(intCursor, after);

      // when
      ArticleSearchCondition cond2 = articleMapper.toCondition(request2, intArticleCursor);

      // then
      assertEquals(request2.cursor(), cond2.cursor().cursor().toString());
    }

    @Test
    @DisplayName("조건에 부합하지 않는 기사가 하나도 없을 경우 빈 Page를 반환한다")
    void shouldReturnEmptyPage_whenNoArticlesMatchCondition() {
      // given
      given(newsArticleRepository.searchByCondition(any())).willReturn(List.of());

      // when
      CursorPageResponseDto<ArticleDto> actual = articleService.getArticleList(requestPublishDate,
          UUID.randomUUID());

      // then
      assertThat(actual)
          .returns(List.of(), CursorPageResponseDto::content)
          .returns(null, CursorPageResponseDto::nextCursor)
          .returns(null, CursorPageResponseDto::nextAfter)
          .returns(0, CursorPageResponseDto::size)
          .returns(0L, CursorPageResponseDto::totalElements)
          .returns(false, CursorPageResponseDto::hasNext);
    }

    @Test
    @DisplayName("키워드 검색 요청을 받으면 옵션에 맞는 커서 페이지를 반환한다")
    void shouldReturnCursorPage_whenSearchByKeyword() {
      // given
      UUID articleId1 = UUID.randomUUID();
      UUID articleId2 = UUID.randomUUID();
      NewsArticle newsArticle1 = NewsArticle.create(naverSource, "link1", "제목1",
          Instant.now().minus(3, ChronoUnit.DAYS), "요약1");
      NewsArticle newsArticle2 = NewsArticle.create(naverSource, "link2", "제목2",
          Instant.now().minus(6, ChronoUnit.DAYS), "요약2");
      NewsArticle newsArticle3 = NewsArticle.create(naverSource, "link3", "제목3",
          Instant.now().minus(9, ChronoUnit.DAYS), "요약3");
      Instant cursorAfter = Instant.now().minus(1, ChronoUnit.HOURS);
      List<NewsArticle> articles = List.of(newsArticle1, newsArticle2, newsArticle3);
      ReflectionTestUtils.setField(newsArticle1, "id", articleId1);
      ReflectionTestUtils.setField(newsArticle2, "id", articleId2);
      ReflectionTestUtils.setField(newsArticle2, "createdAt", cursorAfter);

      given(newsArticleRepository.searchByCondition(any())).willReturn(articles);
      Long totalElements = 3L;
      given(newsArticleRepository.countByCondition(any())).willReturn(totalElements);

      // 읽은 목록
      Set<UUID> viewedArticleIds = Set.of(articleId1);
      given(articleViewRepository.findAllByArticleIdInAndUserId(anyList(), any()))
          .willReturn(viewedArticleIds);
      List<ArticleDto> articleDtoList = List.of(
          articleMapper.toDto(articles.get(0), viewedArticleIds.contains(articleId1)),
          articleMapper.toDto(articles.get(1), viewedArticleIds.contains(articleId2))
      );

      // when
      CursorPageResponseDto<ArticleDto> actual = articleService.getArticleList(requestPublishDate,
          UUID.randomUUID());

      // then
      assertThat(actual)
          .returns(articleDtoList, CursorPageResponseDto::content)
          .returns(articles.get(1).getPublishedAt() + ", " + cursorAfter,
              CursorPageResponseDto::nextCursor)
          .returns(cursorAfter, CursorPageResponseDto::nextAfter)
          .returns(requestPublishDate.limit(), CursorPageResponseDto::size)
          .returns(totalElements, CursorPageResponseDto::totalElements)
          .returns(articles.size() > requestPublishDate.limit(), CursorPageResponseDto::hasNext);
    }

    @Test
    @DisplayName("관심사 검색, 기간 검색에 cursor 요청을 함께 받으면 옵션에 맞는 커서 페이지를 반환한다")
    void shouldReturnCursorPage_whenSearchByInterestAndPeriodWithCursor() {
      // given
      UUID articleId1 = UUID.randomUUID();
      int lastCommentCount = 7;
      Instant createdAt = Instant.now();
      NewsArticle newsArticle1 = NewsArticle.create(naverSource, "link1", "제목1",
          Instant.now().minus(3, ChronoUnit.DAYS), "요약1");
      NewsArticle newsArticle2 = NewsArticle.create(naverSource, "link2", "제목2",
          Instant.now().minus(6, ChronoUnit.DAYS), "요약2");
      List<NewsArticle> articles = List.of(newsArticle1, newsArticle2);
      ReflectionTestUtils.setField(newsArticle1, "id", articleId1);
      ReflectionTestUtils.setField(newsArticle1, "commentCount", lastCommentCount);
      ReflectionTestUtils.setField(newsArticle1, "createdAt", createdAt);

      given(newsArticleRepository.searchByCondition(any())).willReturn(articles);
      Long totalElements = 2L;
      given(newsArticleRepository.countByCondition(any())).willReturn(totalElements);

      // 읽은 목록
      Set<UUID> viewedArticleIds = Set.of(articleId1);
      given(articleViewRepository.findAllByArticleIdInAndUserId(anyList(), any()))
          .willReturn(viewedArticleIds);
      List<ArticleDto> articleDtoList = List.of(
          articleMapper.toDto(articles.get(0), viewedArticleIds.contains(articleId1))
      );

      // when
      CursorPageResponseDto<ArticleDto> actual = articleService.getArticleList(requestCommentCount,
          UUID.randomUUID());

      // then
      assertThat(actual)
          .returns(articleDtoList, CursorPageResponseDto::content)
          .returns(articles.get(0).getCommentCount() + ", " + articles.get(0).getCreatedAt(),
              CursorPageResponseDto::nextCursor)
          .returns(articles.get(0).getCreatedAt(), CursorPageResponseDto::nextAfter)
          .returns(requestCommentCount.limit(), CursorPageResponseDto::size)
          .returns(totalElements, CursorPageResponseDto::totalElements)
          .returns(articles.size() > requestCommentCount.limit(), CursorPageResponseDto::hasNext);
    }

    @Test
    @DisplayName("관심사 검색 요청을 받으면 옵션에 맞는 커서 페이지를 반환한다")
    void shouldReturnCursorPage_whenSearchByInterest() {
      // given
      UUID articleId1 = UUID.randomUUID();
      UUID articleId2 = UUID.randomUUID();
      int lastViewCount = 7;
      Instant createdAt = Instant.now();
      NewsArticle newsArticle1 = NewsArticle.create(naverSource, "link1", "제목1",
          Instant.now().minus(3, ChronoUnit.DAYS), "요약1");
      NewsArticle newsArticle2 = NewsArticle.create(naverSource, "link2", "제목2",
          Instant.now().minus(6, ChronoUnit.DAYS), "요약2");
      List<NewsArticle> articles = List.of(newsArticle1, newsArticle2);
      ReflectionTestUtils.setField(newsArticle1, "id", articleId1);
      ReflectionTestUtils.setField(newsArticle1, "viewCount", lastViewCount);
      ReflectionTestUtils.setField(newsArticle1, "createdAt", createdAt);
      ReflectionTestUtils.setField(newsArticle2, "id", articleId2);

      given(newsArticleRepository.searchByCondition(any())).willReturn(articles);
      Long totalElements = 2L;
      given(newsArticleRepository.countByCondition(any())).willReturn(totalElements);

      // 읽은 목록
      Set<UUID> viewedArticleIds = Set.of(articleId1);
      given(articleViewRepository.findAllByArticleIdInAndUserId(anyList(), any()))
          .willReturn(viewedArticleIds);
      List<ArticleDto> articleDtoList = List.of(
          articleMapper.toDto(articles.get(0), viewedArticleIds.contains(articleId1)),
          articleMapper.toDto(articles.get(1), viewedArticleIds.contains(articleId2))
      );

      // when
      CursorPageResponseDto<ArticleDto> actual = articleService.getArticleList(requestViewCount,
          UUID.randomUUID());

      // then
      assertThat(actual)
          .returns(articleDtoList, CursorPageResponseDto::content)
          .returns(null, CursorPageResponseDto::nextCursor)
          .returns(null, CursorPageResponseDto::nextAfter)
          .returns(articles.size(), CursorPageResponseDto::size)
          .returns(totalElements, CursorPageResponseDto::totalElements)
          .returns(articles.size() > requestViewCount.limit(), CursorPageResponseDto::hasNext);
    }

    @Test
    @DisplayName("잘못된 커서가 들어오면 예외가 발생한다")
    void shouldReturnException_whenInvalidCursor() {
      // given
      ArticleSearchRequest request1 = new ArticleSearchRequest(null, null, null, null, null,
          ArticleOrderBy.PUBLISH_DATE, null, "invalid cursor", null, null);

      // when & then
      assertThrows(BusinessException.class,
          () -> articleService.getArticleList(request1, UUID.randomUUID()));

      // given
      ArticleSearchRequest request2 = new ArticleSearchRequest(null, null, null, null, null,
          ArticleOrderBy.COMMENT_COUNT, null, "invalid cursor2", null, null);

      // when & then
      assertThrows(BusinessException.class,
          () -> articleService.getArticleList(request2, UUID.randomUUID()));
    }
  }

  @Nested
  @DisplayName("뉴스기사 단건 조회를 한다")
  class GetArticle {

    @Test
    @DisplayName("정상적으로 단건 조회 시 조회 이력이 등록되고 기사 정보를 반환한다")
    void shouldReturnArticle_whenValidRequest() {
      // given
      UUID userId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();

      NewsArticle article = NewsArticle.create(naverSource, "link", "제목",
          Instant.now(), "요약");
      ReflectionTestUtils.setField(article, "id", articleId);

      given(newsArticleRepository.findById(articleId)).willReturn(java.util.Optional.of(article));
      given(articleViewService.registerArticleView(articleId, userId))
          .willReturn(null); // void로 바꾸면 이 줄 제거 가능

      ArticleDto expected = articleMapper.toDto(article, true);

      // when
      ArticleDto actual = articleService.getArticle(userId, articleId);

      // then
      assertThat(actual)
          .usingRecursiveComparison()
          .isEqualTo(expected);
      then(articleViewService).should().registerArticleView(articleId, userId);
    }

    @Test
    @DisplayName("존재하지 않는 기사 조회 시 예외가 발생한다")
    void shouldThrowException_whenArticleNotFound() {
      // given
      UUID userId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();

      given(newsArticleRepository.findById(articleId))
          .willReturn(java.util.Optional.empty());

      // when & then
      assertThrows(ArticleNotFoundException.class,
          () -> articleService.getArticle(userId, articleId));
    }

    @Test
    @DisplayName("삭제된 기사 조회 시 예외가 발생한다")
    void shouldThrowException_whenArticleIsDeleted() {
      // given
      UUID userId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();

      NewsArticle article = NewsArticle.create(naverSource, "link", "제목",
          Instant.now(), "요약");
      ReflectionTestUtils.setField(article, "id", articleId);

      // SoftDeleteEntity의 삭제 상태를 테스트용으로 직접 세팅
      ReflectionTestUtils.setField(article, "deleteStatus", DeleteStatus.DELETED);

      given(newsArticleRepository.findById(articleId))
          .willReturn(Optional.of(article));

      // when & then
      assertThrows(DeletedArticleException.class,
          () -> articleService.getArticle(userId, articleId));

      then(articleViewService).shouldHaveNoInteractions();
    }
  }

  @Nested
  @DisplayName("뉴스기사 삭제를 진행한다")
  class DeleteArticle {

    @Test
    @DisplayName("뉴스기사를 찾을 수 없으면 에러를 반환한다")
    void shouldThrowArticleNotFoundException_whenArticleDoesNotExist() {
      // given
      UUID articleId = UUID.randomUUID();
      given(newsArticleRepository.findById(any(UUID.class))).willReturn(Optional.empty());

      // when & then
      assertThrows(ArticleNotFoundException.class,
          () -> articleService.deleteArticle(articleId));
    }

    @Test
    @DisplayName("뉴스기사 상태가 deleted로 되어있는 상태라면 에러를 반환한다")
    void shouldThrowArticleNotFoundException_whenArticleIsDeleted() {
      // given
      UUID articleId = UUID.randomUUID();
      NewsArticle newsArticle = NewsArticle
          .create(naverSource, "link", "title", Instant.now(), "summary");
      ReflectionTestUtils.setField(newsArticle, "deleteStatus", DeleteStatus.DELETED);
      given(newsArticleRepository.findById(any(UUID.class))).willReturn(Optional.of(newsArticle));

      // when & then
      assertThrows(ArticleNotFoundException.class,
          () -> articleService.deleteArticle(articleId));
    }

    @Test
    @DisplayName("뉴스기사 상태를 softDeleted로 만들고 저장한다")
    void shouldSaveArticleAsDeleted_whenLogicalDeleteIsRequested() {
      // given
      UUID articleId = UUID.randomUUID();
      NewsArticle newsArticle = NewsArticle
          .create(naverSource, "link", "title", Instant.now(), "summary");
      given(newsArticleRepository.findById(any(UUID.class))).willReturn(Optional.of(newsArticle));

      // when
      articleService.deleteArticle(articleId);

      // then
      ArgumentCaptor<NewsArticle> captor = ArgumentCaptor.forClass(NewsArticle.class);
      then(newsArticleRepository).should().save(captor.capture());
      assertThat(captor.getValue().getDeleteStatus()).isEqualTo(DeleteStatus.DELETED);
    }
  }

  @Nested
  @DisplayName("뉴스기사 물리삭제를 진행한다")
  class HardDeleteArticle {

    @Test
    @DisplayName("뉴스기사를 찾을 수 없으면 에러를 반환한다")
    void shouldThrowArticleNotFoundException_whenArticleDoesNotExist() {
      // given
      UUID articleId = UUID.randomUUID();
      given(newsArticleRepository.findById(any(UUID.class))).willReturn(Optional.empty());

      // when & then
      assertThrows(ArticleNotFoundException.class,
          () -> articleService.hardDeleteArticle(articleId));
    }

    @Test
    @DisplayName("뉴스기사 삭제 시 연관된 모든 객체를 삭제하는 함수를 호출한다")
    void shouldCallRelatedDataDeleteMethods_whenHardDeleting() {
      // given
      UUID articleId = UUID.randomUUID();
      NewsArticle newsArticle = NewsArticle
          .create(naverSource, "link", "title", Instant.now(), "summary");
      given(newsArticleRepository.findById(any(UUID.class))).willReturn(Optional.of(newsArticle));

      // when
      articleService.hardDeleteArticle(articleId);

      // then
      then(articleInterestRepository).should().deleteAllByArticleId(articleId);
      then(articleViewRepository).should().deleteAllByArticleId(articleId);
      then(commentRepository).should().deleteAllByArticleId(articleId);
      then(newsArticleRepository).should().delete(newsArticle);
    }
  }

  @Nested
  @DisplayName("뉴스기사를 복구한다")
  class RestoreArticle {

    private final ObjectMapper realBackupObjectMapper = new ObjectMapper()
        .configure(SerializationFeature.INDENT_OUTPUT, false)
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private ZoneId zone;
    private LocalDateTime from;
    private LocalDateTime to;

    private ArticleCountInfo info1;
    private ArticleCountInfo info2;
    private List<ArticleCountInfo> infos;

    private ArticleBackupJob job1;
    private ArticleBackupJob job2;
    private List<ArticleBackupJob> jobs;

    private ArticleBackup backup1;
    private ArticleBackup backup12;
    private ArticleBackup backup2;
    private ArticleBackup backup22;


    @BeforeEach
    void setUp() {
      ReflectionTestUtils.setField(articleService, "decompressTaskExecutor",
          new SyncTaskExecutor());
      ReflectionTestUtils.setField(articleService, "backupObjectMapper", realBackupObjectMapper);
      ReflectionTestUtils.setField(articleService, "downloadAndDecompressConcurrency", 3);

      zone = ZoneId.of("Asia/Seoul");
      LocalDate now = LocalDateTime.now(zone).toLocalDate();
      from = now.atStartOfDay(zone).minusDays(2).toLocalDateTime();
      to = now.atStartOfDay(zone).toLocalDateTime();

      LocalDate date = from.toLocalDate();
      info1 = new TestArticleCountInfo(date, 1);
      info2 = new TestArticleCountInfo(date.plusDays(1), 1);
      infos = List.of(info1, info2);

      job1 = ArticleBackupJob.create(date, BackupJobType.ARTICLE_DAILY_BACKUP)
          .setArticleCount(2);
      job2 = ArticleBackupJob.create(date.plusDays(1), BackupJobType.ARTICLE_DAILY_BACKUP)
          .setArticleCount(2);
      jobs = List.of(job1, job2);

    }

    @Test
    @DisplayName("기간 조회시 잘못된 기간이 들어오면 에러를 반환한다")
    void shouldThrowInvalidPeriodException_whenInvalidPeriodIsGiven() {
      // given
      from = LocalDateTime.MAX;
      to = LocalDateTime.MIN;

      // when
      assertThrows(ArticleInvalidPeriodException.class,
          () -> articleService.restoreArticle(from, to));
    }

    @Test
    @DisplayName("기간 조회시 복구해야할 데이터가 없으면 빈 배열을 반환한다")
    void shouldReturnEmptyList_whenNoMissingDataInPeriod() {
      // given
      LocalDate date = from.toLocalDate();
      ArticleCountInfo info1 = new TestArticleCountInfo(date, 2);
      ArticleCountInfo info2 = new TestArticleCountInfo(date.plusDays(1), 2);
      List<ArticleCountInfo> infos = List.of(info1, info2);
      given(newsArticleRepository.countNewsArticlesByPublishDate(
          any(Instant.class), any(Instant.class))).willReturn(infos);

      ArticleBackupJob job1 = ArticleBackupJob.create(date, BackupJobType.ARTICLE_DAILY_BACKUP)
          .setArticleCount(2);
      ArticleBackupJob job2 = ArticleBackupJob.create(date.plusDays(1),
              BackupJobType.ARTICLE_DAILY_BACKUP)
          .setArticleCount(2);
      List<ArticleBackupJob> jobs = List.of(job1, job2);
      given(articleBackupJobRepository.findAllByBackupDateBetweenAndJobTypeAndStatus(
          any(LocalDate.class), any(LocalDate.class), any(BackupJobType.class),
          any(BackupJobStatus.class))).willReturn(jobs);

      // when
      List<ArticleRestoreResultDto> actual = articleService.restoreArticle(from, to);

      // then
      assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("S3 다운로드 실패 시 recordFailed 로그가 기록되어야 한다")
    void shouldRecordFailed_whenS3ErrorOccurs() throws Exception {
      // given
      given(newsArticleRepository.countNewsArticlesByPublishDate(any(), any())).willReturn(infos);
      given(articleBackupJobRepository.findAllByBackupDateBetweenAndJobTypeAndStatus(
          any(), any(), any(), any())).willReturn(jobs);

      ResponseInputStream<GetObjectResponse> successStream = mock(ResponseInputStream.class);
      given(s3AsyncClient.getObject(any(Consumer.class), any(AsyncResponseTransformer.class)))
          .willReturn(CompletableFuture.completedFuture(successStream))
          .willReturn(CompletableFuture.failedFuture(new IOException("S3 Connection Timeout")));
      ArticleBackup mock = mock(ArticleBackup.class);
      willReturn(List.of(mock)).given(articleService)
          .decompressGzipAndReturnArticlesToRestore(any(), anyInt(), any());

      // when
      articleService.restoreArticle(from, to);

      // then
      then(articleBackupJobLogService)
          .should(timeout(2000).atLeastOnce())
          .recordRestoreFailed(any(), contains("gzip 스트리밍 처리중 에러 발생"));
    }

    @Test
    @DisplayName("모든 프로세스(다운로드, 압축해제) 실패 시 빠른 종료를 한다")
    void shouldFastFail_whenAllProcessesFail() throws Exception {
      // given
      given(newsArticleRepository.countNewsArticlesByPublishDate(any(), any())).willReturn(infos);
      given(articleBackupJobRepository.findAllByBackupDateBetweenAndJobTypeAndStatus(
          any(), any(), any(), any())).willReturn(jobs);

      ResponseInputStream<GetObjectResponse> invalidFileStream = mock(ResponseInputStream.class);
      given(s3AsyncClient.getObject(any(Consumer.class), any(AsyncResponseTransformer.class)))
          .willReturn(CompletableFuture.completedFuture(invalidFileStream))
          .willReturn(CompletableFuture.failedFuture(new IOException("S3 Connection Timeout")));

      // when
      List<ArticleRestoreResultDto> actual = articleService.restoreArticle(from, to);

      // then
      then(articleBackupJobLogService)
          .should(timeout(2000).atLeastOnce())
          .recordRestoreFailed(any(), contains("에러 발생"));
      then(articleBatchService).shouldHaveNoInteractions();
      assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("배치 저장중 실패하면 오류 반환 및 에러를 기록하고 정상 진행한다")
    void shouldRecordErrorAndContinue_whenBatchSaveFails() {
      // given
      given(newsArticleRepository.countNewsArticlesByPublishDate(any(), any())).willReturn(infos);
      given(articleBackupJobRepository.findAllByBackupDateBetweenAndJobTypeAndStatus(
          any(), any(), any(), any())).willReturn(jobs);

      ResponseInputStream<GetObjectResponse> successStream = mock(ResponseInputStream.class);
      given(s3AsyncClient.getObject(any(Consumer.class), any(AsyncResponseTransformer.class)))
          .willReturn(CompletableFuture.completedFuture(successStream))
          .willReturn(CompletableFuture.failedFuture(new IOException("S3 Connection Timeout")));
      ArticleBackup mock = mock(ArticleBackup.class);
      willReturn(List.of(mock)).given(articleService)
          .decompressGzipAndReturnArticlesToRestore(any(), anyInt(), any());
      given(articleBatchService.saveRestoredArticlesAndLog(anyList(), any()))
          .willThrow(new RuntimeException("저장 에러"));

      // when
      articleService.restoreArticle(from, to);

      // then
      then(articleBackupJobLogService)
          .should(atLeastOnce())
          .recordRestoreFailed(any(), contains("저장중 오류"));
    }

    @Test
    @DisplayName("복구요청을 할 때 뉴스기사를 에러없이 성공적으로 복구한다")
    void shouldRestoreArticlesSuccessfullyWithoutAnyError_whenRestoreRequests() throws Exception {
      // given
      given(newsArticleRepository.countNewsArticlesByPublishDate(any(), any())).willReturn(infos);
      given(articleBackupJobRepository.findAllByBackupDateBetweenAndJobTypeAndStatus(
          any(), any(), any(), any())).willReturn(jobs);

      Instant pub1 = from.toLocalDate().atStartOfDay(zone).toInstant().plusSeconds(300);
      Instant pub2 = pub1.plus(1, ChronoUnit.DAYS);
      ArticleLinkAndPublishedAt linkAndPublishedAt1 = new TestArticleLinkAndPublishedAt("link1",
          pub1);
      ArticleLinkAndPublishedAt linkAndPublishedAt2 = new TestArticleLinkAndPublishedAt("link2",
          pub2);
      Set<ArticleLinkAndPublishedAt> listPubs = Set.of(linkAndPublishedAt1, linkAndPublishedAt2);
      given(newsArticleRepository.findLinksByPublishDate(any(), any(), anyCollection()))
          .willReturn(listPubs);

      // 1번째 LocalDate
      backup1 = new ArticleBackup(NewsSourceType.NAVER, "link1", "title1", pub1, "summary1");
      backup12 = new ArticleBackup(NewsSourceType.NAVER, "link1-2", "title1-2",
          pub1.plusSeconds(10), "summary1-2");
      String backupJsonl1 = realBackupObjectMapper.writeValueAsString(backup1) + "\n"
          + realBackupObjectMapper.writeValueAsString(backup12);
      byte[] gzip1 = createGzipByteArray(backupJsonl1);

      // 2번째 LocalDate
      backup2 = new ArticleBackup(NewsSourceType.NAVER, "link2", "title2", pub2, "summary2");
      backup22 = new ArticleBackup(NewsSourceType.NAVER, "link2-2", "title2-2",
          pub2.plusSeconds(100), "summary2-2");
      String backupJsonl2 = realBackupObjectMapper.writeValueAsString(backup2) + "\n"
          + realBackupObjectMapper.writeValueAsString(backup22);
      byte[] gzip2 = createGzipByteArray(backupJsonl2);

      GetObjectResponse response = GetObjectResponse.builder().build();
      ResponseInputStream<GetObjectResponse> successStream1 =
          new ResponseInputStream<>(response, new ByteArrayInputStream(gzip1));
      ResponseInputStream<GetObjectResponse> successStream2 =
          new ResponseInputStream<>(response, new ByteArrayInputStream(gzip2));

      given(s3AsyncClient.getObject(any(Consumer.class), any(AsyncResponseTransformer.class)))
          .willReturn(CompletableFuture.completedFuture(successStream1))
          .willReturn(CompletableFuture.completedFuture(successStream2));

      given(articleBatchService.saveRestoredArticlesAndLog(any(), any()))
          .willReturn(List.of(UUID.randomUUID()))
          .willReturn(List.of(UUID.randomUUID()));

      // when
      List<ArticleRestoreResultDto> actual = articleService.restoreArticle(from, to);

      // then
      assertThat(actual)
          .extracting(ArticleRestoreResultDto::restoredArticleIds)
          .flatExtracting(ids -> ids)
          .hasSize(2);
    }

    private byte[] createGzipByteArray(String data) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
        gos.write(data.getBytes(StandardCharsets.UTF_8));
      }
      return baos.toByteArray();
    }

    static class TestArticleCountInfo implements ArticleCountInfo {

      private final LocalDate date;
      private final long count;

      public TestArticleCountInfo(LocalDate date, long count) {
        this.date = date;
        this.count = count;
      }

      @Override
      public LocalDate getLocalDate() {
        return date;
      }

      @Override
      public Long getCount() {
        return count;
      }
    }

    static class TestArticleLinkAndPublishedAt implements ArticleLinkAndPublishedAt {

      private final String link;
      private final Instant publishedAt;

      public TestArticleLinkAndPublishedAt(String link, Instant publishedAt) {
        this.link = link;
        this.publishedAt = publishedAt;
      }

      @Override
      public String getLink() {
        return link;
      }

      @Override
      public Instant getPublishedAt() {
        return publishedAt;
      }
    }
  }
}

package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.team3.monew.config.AwsProperties;
import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.dto.article.internal.enums.ArticleDirection;
import com.team3.monew.dto.article.internal.enums.ArticleOrderBy;
import com.team3.monew.entity.ArticleInterest;
import com.team3.monew.entity.ArticleView;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.CommentLike;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.DeleteStatus;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.ArticleBackupJobRepository;
import com.team3.monew.repository.ArticleInterestRepository;
import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.CommentLikeRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import com.team3.monew.repository.UserRepository;
import com.team3.monew.service.ArticleBackupJobLogService;
import com.team3.monew.service.ArticleBatchService;
import com.team3.monew.support.IntegrationTestSupport;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("external-api")
@TestPropertySource(locations = "file:.env")
@EnabledIfEnvironmentVariable(named = "ENV", matches = "dev")
public class ArticleServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private NewsArticleRepository newsArticleRepository;
  @Autowired
  private NewsSourceRepository newsSourceRepository;
  @Autowired
  private InterestRepository interestRepository;
  @Autowired
  private ArticleViewRepository articleViewRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private CommentRepository commentRepository;
  @Autowired
  private CommentLikeRepository commentLikeRepository;
  @Autowired
  private ArticleInterestRepository articleInterestRepository;
  @Autowired
  private ArticleMapper articleMapper;
  @Autowired
  private AwsProperties awsProperties;
  @Autowired
  private S3Client s3Client;
  @Autowired
  private ObjectMapper backupObjectMapper;
  @Autowired
  private ArticleBackupJobRepository articleBackupJobRepository;
  @Autowired
  private ArticleBatchService articleBatchService;

  @Autowired
  private MockMvc mockMvc;

  private static final String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";
  private static final String ARTICLES_BASE_URL = "/api/articles";

  private NewsArticle newsArticle;
  private UUID commentLikeId;

  private NewsArticle newsArticle1;
  private NewsArticle newsArticle2;
  private NewsArticle newsArticle3;
  private NewsArticle newsArticle4;
  private NewsArticle newsArticle5;
  private NewsArticle newsArticle6;
  @Autowired
  private ArticleBackupJobLogService articleBackupJobLogService;


  @BeforeEach
  void setUp() {
    Interest samsungInterest = Interest.create("삼성");
    Interest appleInterest = Interest.create("애플");
    interestRepository.saveAll(List.of(samsungInterest, appleInterest));

    NewsSource naverSource = newsSourceRepository.findByName(NewsSourceType.NAVER.name())
        .orElseGet(() -> NewsSource.create
            (NewsSourceType.NAVER.name(), NewsSourceType.NAVER, "baseUrl"));

    newsArticle = NewsArticle
        .create(naverSource, "link", "title", Instant.now(), "summary");
    newsArticle.addArticleInterest(samsungInterest, "갤럭시");
    newsArticle.addArticleInterest(samsungInterest, "메모리");
    newsArticle.addArticleInterest(appleInterest, "아이폰");
    newsArticleRepository.save(newsArticle);
    User user1 = User.create("email@naver.com", "닉닉", "@qwer!!");
    User user2 = User.create("user2@gmail.com", "ha", "orange@1234");
    userRepository.saveAll(List.of(user1, user2));

    ArticleView articleView1 = ArticleView.create(newsArticle, user1);
    ArticleView articleView2 = ArticleView.create(newsArticle, user2);
    articleViewRepository.saveAll(List.of(articleView1, articleView2));

    Comment u1comment = Comment.create(newsArticle, user1, "comment1");
    Comment u2comment = Comment.create(newsArticle, user2, "comment2");
    commentRepository.saveAll(List.of(u1comment, u2comment));

    CommentLike u2CommentLike = CommentLike.create(u2comment, user2);
    commentLikeRepository.save(u2CommentLike);
    commentLikeId = u2CommentLike.getId();

    // restore Data Set
    ZoneId zone = ZoneId.of("Asia/Seoul");
    LocalDate nowDateMinusSeven = LocalDate.now(zone).minusDays(7);
    Instant startAt = nowDateMinusSeven.atStartOfDay(zone).toInstant();
    Instant startPlusOneAt = startAt.plus(1, ChronoUnit.DAYS);

    newsArticle1 = NewsArticle.create(
        naverSource, "originalLink1", "title1", startAt.minus(2, ChronoUnit.HOURS),
        "summary1");
    newsArticle2 = NewsArticle.create(
        naverSource, "originalLink2", "title2", startAt, "summary2");
    newsArticle3 = NewsArticle.create(
        naverSource, "originalLink3", "title3", startAt.plus(7, ChronoUnit.HOURS),
        "summary3");
    newsArticle4 = NewsArticle.create(
        naverSource, "originalLink4", "title4", startPlusOneAt.minusMillis(1), "summary4");
    newsArticle5 = NewsArticle.create(
        naverSource, "originalLink5", "title5", startPlusOneAt, "summary5");
    newsArticle6 = NewsArticle.create(
        naverSource, "originalLink6", "title6", startPlusOneAt.plusMillis(1), "summary6");
    List<NewsArticle> articles = List.of(newsArticle1, newsArticle2, newsArticle3, newsArticle4,
        newsArticle5, newsArticle6);
    newsArticleRepository.saveAll(articles);
  }

  @AfterEach
  void tearDown() {
    articleInterestRepository.deleteAll();
    commentLikeRepository.deleteAll();
    commentRepository.deleteAll();
    articleViewRepository.deleteAll();
    newsArticleRepository.deleteAll();
    userRepository.deleteAll();
    interestRepository.deleteAll();
    articleBackupJobRepository.deleteAll();
  }

  @Test
  @DisplayName("뉴스기사 목록 조회 통합 테스트에 성공합니다")
  void shouldReturnArticlePage_whenAPICalls() throws Exception {
    // when & then
    mockMvc.perform(get("/api/articles")
            .header(REQUEST_USER_ID_HEADER, UUID.randomUUID())
            .param("keyword", "삼성 메모리")
            .param("sourceIn", NewsSourceType.NAVER.name())
            .param("orderBy", ArticleOrderBy.PUBLISH_DATE.toString())
            .param("direction", ArticleDirection.DESC.toString())
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(0))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("뉴스기사 단건 조회 시 동일 사용자 재조회에서는 조회수가 증가하지 않는다")
  void shouldNotIncreaseViewCount_whenSameUserViewsTwice() throws Exception {
    // given
    User user = userRepository.saveAndFlush(
        User.create("view-test@example.com", "tester", "test1234!")
    );
    UUID userId = user.getId();
    UUID articleId = newsArticle.getId();

    // when
    mockMvc.perform(get("/api/articles/{articleId}", articleId)
            .header(REQUEST_USER_ID_HEADER, userId))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/articles/{articleId}", articleId)
            .header(REQUEST_USER_ID_HEADER, userId))
        .andExpect(status().isOk());

    // then
    NewsArticle updated = newsArticleRepository.findById(articleId).orElseThrow();
    assertThat(updated.getViewCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("삭제된 기사 조회 시 400 오류가 발생한다")
  void shouldReturn400_whenArticleIsDeleted() throws Exception {
    // given
    User user = userRepository.saveAndFlush(
        User.create("deleted-article-test@example.com", "tester", "test1234!")
    );
    UUID userId = user.getId();
    UUID articleId = newsArticle.getId();

    ReflectionTestUtils.setField(newsArticle, "deleteStatus", DeleteStatus.DELETED);
    newsArticleRepository.saveAndFlush(newsArticle);

    // when & then
    mockMvc.perform(get("/api/articles/{articleId}", articleId)
            .header(REQUEST_USER_ID_HEADER, userId))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("뉴스기사 논리삭제 성공 시 204 반환과 함께 기사의 상태가 softDeleted 상태여야 한다")
  void shouldReturnNoContentAndSoftDeleteArticle_whenDeleteSuccessful() throws Exception {
    // when & then
    mockMvc.perform(delete(ARTICLES_BASE_URL + "/{articleId}", newsArticle.getId()))
        .andExpect(status().isNoContent());

    NewsArticle findNewsArticle = newsArticleRepository.findById(newsArticle.getId())
        .orElseThrow(RuntimeException::new);
    assertThat(findNewsArticle.getDeleteStatus()).isEqualTo(DeleteStatus.DELETED);

    List<ArticleInterest> articleInterests = articleInterestRepository
        .findAllByArticleId(findNewsArticle.getId());
    assertThat(articleInterests).isNotEmpty();

    List<ArticleView> articleViews = articleViewRepository
        .findAllByArticleId(findNewsArticle.getId());
    assertThat(articleViews).isNotEmpty();

    List<Comment> comments = commentRepository.findAllByArticleId(findNewsArticle.getId());
    assertThat(comments).isNotEmpty();
  }

  @Test
  @DisplayName("뉴스기사 물리삭제 성공 시 204 반환과 함께 기사는 DB에서 찾을 수 없어야 한다")
  void shouldReturnNoContentAndDeleteArticle_whenDeletedPhysically() throws Exception {
    // when & then
    mockMvc.perform(delete(ARTICLES_BASE_URL + "/{articleId}/hard", newsArticle.getId()))
        .andExpect(status().isNoContent());

    Optional<NewsArticle> findNewsArticle = newsArticleRepository.findById(newsArticle.getId());
    assertThat(findNewsArticle).isEmpty();

    List<ArticleInterest> articleInterests = articleInterestRepository
        .findAllByArticleId(newsArticle.getId());
    assertThat(articleInterests).isEmpty();

    List<ArticleView> articleViews = articleViewRepository
        .findAllByArticleId(newsArticle.getId());
    assertThat(articleViews).isEmpty();

    List<Comment> comments = commentRepository.findAllByArticleId(newsArticle.getId());
    Optional<CommentLike> commentLike = commentLikeRepository.findById(commentLikeId);
    assertThat(comments).isEmpty();
    assertThat(commentLike).isEmpty();
  }

  @Test
  @DisplayName("논리삭제를 진행할 때 삭제할 기사가 존재하지 않으면 404 NotFound를 반환한다")
  void shouldReturnNotFound_whenSoftDeletingNonExistentArticle() throws Exception {
    // given
    UUID articleId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete(ARTICLES_BASE_URL + "/{articleId}", articleId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ARTICLE_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value("404"))
        .andExpect(jsonPath("$.details.articleId").value(articleId.toString()));
  }

  @Test
  @DisplayName("물리삭제를 진행할 때 삭제할 기사가 존재하지 않으면 404 NotFound를 반환한다")
  void shouldReturnNotFound_whenHardDeletingNonExistentArticle() throws Exception {
    // given
    UUID articleId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete(ARTICLES_BASE_URL + "/{articleId}/hard", articleId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ARTICLE_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value("404"))
        .andExpect(jsonPath("$.details.articleId").value(articleId.toString()));
  }

  @Test
  @DisplayName("정해진 기간이 주어질 때 데이터를 확인하고 유실된 데이터가 없으면 빈 배열을 반환한다")
  void shouldReturnEmptyList_whenNoMissingDataInPeriod() throws Exception {
    // given
    ZoneId zone = ZoneId.of("Asia/Seoul");
    LocalDateTime start = LocalDate.now(zone).minusDays(3).atStartOfDay();
    LocalDateTime end = LocalDate.now(zone).minusDays(1).atStartOfDay();

    // when & then
    mockMvc.perform(get(ARTICLES_BASE_URL + "/restore")
            .param("from", start.toString())
            .param("to", end.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  @DisplayName("정해진 기간이 주어질 때 그 안의 데이터를 확인하고 유실된 데이터를 복구해서 결과를 반환한다")
  void shouldReturnRestoredArticles_whenPeriodIsGiven() throws Exception {
    // given
    ZoneId zone = ZoneId.of("Asia/Seoul");
    LocalDate nowDateMinusTen = LocalDate.now(zone).minusDays(10);
    LocalDateTime start = nowDateMinusTen.atStartOfDay();
    LocalDateTime end = nowDateMinusTen.plusDays(7).atStartOfDay();
    Map<LocalDate, List<NewsArticle>> articlesByDate = compressAndS3Upload(start, end);
    List<NewsArticle> deletedArticles = deleteArticles(articlesByDate);

    // when
    MvcResult result = mockMvc.perform(get(ARTICLES_BASE_URL + "/restore")
            .param("from", start.toString())
            .param("to", end.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andReturn();

    // then
    String content = result.getResponse().getContentAsString();
    List<String> allExtractIds = JsonPath.read(content, "$.[*].restoredArticleIds[*]");
    List<UUID> ids = allExtractIds.stream().map(UUID::fromString).toList();
    List<NewsArticle> restoredArticles = newsArticleRepository.findAllById(ids);

    assertThat(restoredArticles)
        .extracting("originalLink", "title", "publishedAt", "summary")
        .containsExactlyInAnyOrderElementsOf(
            deletedArticles.stream()
                .map(
                    a -> tuple(a.getOriginalLink(),
                        a.getTitle(), a.getPublishedAt(), a.getSummary()))
                .toList()
        );
  }

  private List<NewsArticle> deleteArticles(Map<LocalDate, List<NewsArticle>> articlesByDate) {
    List<NewsArticle> deletedArticles = new ArrayList<>();
    List<NewsArticle> toDeleteTotal = new ArrayList<>();
    articlesByDate.forEach((date, articles) -> {
      if (articles != null && !articles.isEmpty()) {
        NewsArticle deleteTarget = articles.remove(0);
        toDeleteTotal.add(deleteTarget);
        deletedArticles.add(deleteTarget);
      }
    });
    newsArticleRepository.deleteAll(toDeleteTotal);

    return deletedArticles;
  }

  private Map<LocalDate, List<NewsArticle>> compressAndS3Upload(LocalDateTime start,
      LocalDateTime end) {
    ZoneId zone = ZoneId.of("Asia/Seoul");
    Instant startAt = start.atZone(zone).toInstant();
    Instant endAt = end.atZone(zone).toInstant();
    Map<LocalDate, List<NewsArticle>> articlesByDate = newsArticleRepository
        .findAllByPublishedAtGreaterThanEqualAndPublishedAtLessThan(startAt, endAt)
        .stream()
        .collect(Collectors.groupingBy(
            article -> article.getPublishedAt().atZone(zone).toLocalDate(),
            Collectors.mapping(article -> article, Collectors.toList())
        ));

    String bucket = awsProperties.getS3().getBucket();
    articlesByDate.forEach((date, articles) -> {
      String key =
          "test/backup-integration/test-backup-" + date.toString() + ".jsonl.gz";
      UUID jobId = articleBackupJobLogService.createBackupJob(date, bucket, key);
      articleBackupJobLogService.recordSuccess(jobId, articles.size());

      List<ArticleBackup> backups = articles.stream().map(articleMapper::toBackupDto).toList();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream gzos = new GZIPOutputStream(baos);
          BufferedWriter writer = new BufferedWriter(
              new OutputStreamWriter(gzos, StandardCharsets.UTF_8))) {

        for (ArticleBackup backup : backups) {
          String jsonLine = backupObjectMapper.writeValueAsString(backup);
          writer.write(jsonLine);
          writer.newLine(); // JSONL의 핵심: 줄바꿈
        }
        writer.flush();
      } catch (Exception e) {
        throw new RuntimeException("메모리 압축 중 에러", e);
      }

      byte[] compressedData = baos.toByteArray();

      s3Client.putObject(req -> req.bucket(bucket).key(key)
              .contentEncoding("gzip").contentType("application/x-jsonlines"),
          RequestBody.fromBytes(compressedData));
    });

    return articlesByDate;
  }
}

package com.team3.monew.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.dto.article.ArticleDto;
import com.team3.monew.dto.article.ArticleRestoreResultDto;
import com.team3.monew.dto.article.ArticleSearchRequest;
import com.team3.monew.dto.article.internal.ArticleCursor;
import com.team3.monew.dto.article.internal.ArticleSearchCondition;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.entity.ArticleBackupJob;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.enums.BackupJobStatus;
import com.team3.monew.entity.enums.BackupJobType;
import com.team3.monew.exception.article.ArticleInvalidPeriodException;
import com.team3.monew.exception.article.ArticleNotFoundException;
import com.team3.monew.exception.article.DeletedArticleException;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.global.exception.BusinessException;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.ArticleBackupJobRepository;
import com.team3.monew.repository.ArticleInterestRepository;
import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsArticleRepository.ArticleCountInfo;
import com.team3.monew.repository.NewsArticleRepository.ArticleLinkAndPublishedAt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

  private final NewsArticleRepository newsArticleRepository;
  private final ArticleViewRepository articleViewRepository;
  private final ArticleViewService articleViewService;
  private final ArticleInterestRepository articleInterestRepository;
  private final CommentRepository commentRepository;
  private final ArticleBackupJobRepository articleBackupJobRepository;
  private final ArticleBatchService articleBatchService;

  private final S3AsyncClient s3AsyncClient;
  private final ArticleMapper articleMapper;
  private final TaskExecutor decompressTaskExecutor;
  private final ObjectMapper backupObjectMapper;

  @Value("${app.restore.concurrency:3}")
  private int downloadAndDecompressConcurrency;
  private static final String CURSOR_DELIMITER = ", ";
  private final ArticleBackupJobLogService articleBackupJobLogService;

  public CursorPageResponseDto<ArticleDto> getArticleList(
      ArticleSearchRequest request, UUID requestUserId) {
    log.debug("뉴스 목록 조회 요청 - keyword={}, interestid={}", request.keyword(), request.interestId());
    ArticleCursor cursor = parseCursor(request);
    ArticleSearchCondition searchCondition = articleMapper.toCondition(request, cursor);

    List<NewsArticle> articles = newsArticleRepository.searchByCondition(searchCondition);

    if (articles.isEmpty()) {
      log.debug("뉴스 목록 조회 성공 - keyword={}, interestid={}, size=0",
          request.keyword(), request.interestId());
      return new CursorPageResponseDto<>(Collections.emptyList(), null, null,
          0, (long) articles.size(), false);
    }

    Long totalElements = newsArticleRepository.countByCondition(searchCondition);
    boolean hasNext = articles.size() > request.limit();
    Object nextCursor = null;
    Instant nextAfter = null;
    if (hasNext) {
      articles = articles.subList(0, request.limit());
      NewsArticle lastArticle = articles.get(articles.size() - 1);
      nextCursor = switch (request.orderBy()) {
        case PUBLISH_DATE -> lastArticle.getPublishedAt();
        case COMMENT_COUNT -> lastArticle.getCommentCount();
        case VIEW_COUNT -> lastArticle.getViewCount();
      };
      // 커서(커서 반환값 + ", " + 커서필드 객체의 생성시간)
      nextCursor = nextCursor.toString() + CURSOR_DELIMITER + lastArticle.getCreatedAt();
      nextAfter = lastArticle.getCreatedAt();
    }
    log.debug("다음 커서 생성 완료: nextCursor={}, nextAfter={}", nextCursor, nextAfter);

    // article
    List<UUID> articleIds = articles.stream().map(NewsArticle::getId).toList();
    // requestUser가 읽은 기사 목록
    Set<UUID> viewedArticleIds = articleViewRepository.findAllByArticleIdInAndUserId(
        articleIds, requestUserId);

    List<ArticleDto> articleDtoList = articles.stream()
        .map(na -> articleMapper.toDto(na, viewedArticleIds.contains(na.getId())))
        .toList();

    log.debug("뉴스 목록 조회 성공 - keyword={}, interestid={}, size={}",
        request.keyword(), request.interestId(), articleDtoList.size());
    return new CursorPageResponseDto<>(articleDtoList,
        nextCursor != null ? nextCursor.toString() : null,
        nextAfter, articleDtoList.size(), totalElements, hasNext);
  }

  // 조회수 등록 때문에 readOnly=true 불가능으로 어노테이션 추가함
  @Transactional
  public ArticleDto getArticle(UUID userId, UUID articleId) {
    log.debug("뉴스 단건 조회 요청 - articleId={}", articleId);
    NewsArticle article = findActiveArticleOrElseThrow(articleId);
    // 단건 조회에도 조회수 등록을 위함
    articleViewService.registerArticleView(article.getId(), userId);

    // registerArticleView()에서 벌크 업데이트 수행으로 기존 엔티티의 viewCount가 갱신되지 않아 재조회
    NewsArticle updatedArticle = findActiveArticleOrElseThrow(articleId);

    log.debug("뉴스 단건 조회 성공 - articleId={}", updatedArticle.getId());
    return articleMapper.toDto(updatedArticle, true);
  }

  @Transactional
  public void deleteArticle(UUID articleId) {
    log.debug("뉴스기사 논리삭제 요청 - articleId={}", articleId);
    NewsArticle article = getArticleOrThrow(articleId);
    if (article.isDeleted()) {
      throw new ArticleNotFoundException(articleId);
    }

    article.markDeleted();
    newsArticleRepository.save(article);
    log.info("뉴스기사 논리삭제 성공 - articleId={}", articleId);
  }

  @Transactional
  public void hardDeleteArticle(UUID articleId) {
    log.debug("뉴스기사 물리삭제 요청 - articleId={}", articleId);
    NewsArticle newsArticle = getArticleOrThrow(articleId);

    // 1. ArticleInterest 삭제
    articleInterestRepository.deleteAllByArticleId(articleId);
    // 2. ArticleViews 삭제
    articleViewRepository.deleteAllByArticleId(articleId);
    // 3. Comments 삭제
    commentRepository.deleteAllByArticleId(articleId);

    newsArticleRepository.delete(newsArticle);
    log.info("뉴스기사 물리삭제 성공 - articleId={}", articleId);
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public List<ArticleRestoreResultDto> restoreArticle(LocalDateTime from, LocalDateTime to) {
    log.debug("뉴스기사 복구 요청 - from={}, to={}", from.toLocalDate(), to.toLocalDate());
    if (from.isAfter(to)) {
      throw new ArticleInvalidPeriodException();
    }

    ZoneId zone = ZoneId.of("Asia/Seoul");
    Instant startAt = from.toLocalDate().atStartOfDay(zone)
        .toInstant();   // From날의 시작지점(이상)
    Instant endAt = to.toLocalDate().plusDays(1).atStartOfDay(zone)
        .toInstant();   // To날의 다음날 시작지점(미만)

    // DB에 저장된 날짜별 기사 개수
    Map<LocalDate, Integer> countByDate = newsArticleRepository
        .countNewsArticlesByPublishDate(startAt, endAt).stream()
        .collect(Collectors.toMap(ArticleCountInfo::getLocalDate, ArticleCountInfo::getCount));
    Set<LocalDate> dates = countByDate.keySet();

    // 날짜별 기사 개수와 저장된 백업 기사개수가 다른 복구해야할 항목들
    List<ArticleBackupJob> jobsToRestore = articleBackupJobRepository.findAllByBackupDateBetweenAndJobTypeAndStatus(
            from.toLocalDate(), to.toLocalDate(),
            BackupJobType.ARTICLE_DAILY_BACKUP, BackupJobStatus.SUCCESS)
        .stream()
        .filter(abj -> {
          Integer actualCount = countByDate.getOrDefault(abj.getBackupDate(), 0);
          return actualCount < abj.getArticleCount();
        })
        .toList();

    if (jobsToRestore.isEmpty()) {
      log.info("뉴스기사 복구 대상 없음 - from={}, to={}", from.toLocalDate(), to.toLocalDate());
      return List.of();
    }

    // 날짜별 복구해야하는 기사 개수
    Map<LocalDate, Integer> articleCountToRestoreByDate = jobsToRestore.stream()
        .collect(Collectors.toMap(
            ArticleBackupJob::getBackupDate,
            abj -> {
              int backupCount = abj.getArticleCount(); // 백업된 데이터 개수
              int DBCount = countByDate.getOrDefault(abj.getBackupDate(), 0); // 실제 가지고 있는 데이터 개수
              return backupCount - DBCount;
            }
        ));
    Map<LocalDate, UUID> restoreJobIdsByDate = articleBackupJobLogService
        .createRestoreJobAll(articleCountToRestoreByDate.keySet());

    List<LocalDate> sortedDates = jobsToRestore.stream()
        .map(ArticleBackupJob::getBackupDate)
        .sorted()
        .toList();
    Instant startOfDates = sortedDates.get(0).atStartOfDay(zone).toInstant();
    Instant endOfDates = sortedDates.get(sortedDates.size() - 1).atStartOfDay(zone)
        .plusDays(1).toInstant();

    // 복구가 필요한 기간의 저장된 날짜별 기사 Links
    Map<LocalDate, Set<String>> existingLinksByDate = newsArticleRepository.findLinksByPublishDate(
            startOfDates, endOfDates, sortedDates).stream()
        .collect(Collectors.groupingBy(
            article -> article.getPublishedAt().atZone(zone).toLocalDate(),
            Collectors.mapping(ArticleLinkAndPublishedAt::getLink, Collectors.toSet())
        ));

    // 날짜별 복구해야할 기사목록
    Map<LocalDate, List<ArticleBackup>> articlesToRestoreByDate = Flux.fromIterable(jobsToRestore)
        .flatMap(job -> {
          CompletableFuture<List<ArticleBackup>> future =
              s3AsyncClient.getObject(req -> req.key(job.getS3Key()).bucket(job.getS3Bucket()),
                      AsyncResponseTransformer.toBlockingInputStream())   // InputStream 형
                  .thenApplyAsync(inputStream -> {
                    LocalDate localDate = job.getBackupDate();
                    int limit = articleCountToRestoreByDate.getOrDefault(localDate, 0);
                    Set<String> existingLinks = existingLinksByDate.getOrDefault(localDate,
                        Set.of());
                    return decompressGzipAndReturnArticlesToRestore( // 압축해제 및 List반환
                        inputStream, limit, existingLinks);
                  }, decompressTaskExecutor);

          return Mono.fromFuture(future)
              .map(articles -> Map.entry(job.getBackupDate(), articles))
              .onErrorResume(e -> {
                String errorMessage;
                if (e instanceof IOException || e.getCause() instanceof IOException) {
                  errorMessage = "gzip 스트리밍 처리중 에러 발생";
                } else {
                  errorMessage = "알 수 없는 시스템 에러 발생";
                }

                LocalDate date = job.getBackupDate();
                UUID restoreJobId = restoreJobIdsByDate.get(date);

                return Mono.fromRunnable(() -> {
                      articleBackupJobLogService.recordRestoreFailed(restoreJobId, errorMessage);
                      log.error("뉴스기사 복구 실패 - date={}, errorMessage={}", date, errorMessage, e);
                    })
                    .subscribeOn(Schedulers.boundedElastic())     // 기록해야해서 다른 스레드 풀에서 실행
                    .then(Mono.just(Map.entry(date, List.of()))); // 작업 끝나고 빈 결과 반환
              });
        }, downloadAndDecompressConcurrency)
        .collectMap(Map.Entry::getKey, Map.Entry::getValue)
        .blockOptional()
        .orElseGet(Map::of);

    List<ArticleBackup> allArticles = articlesToRestoreByDate.values().stream()
        .flatMap(List::stream)
        .toList();
    if (allArticles.isEmpty()) {
      log.error("뉴스기사 최종 복구 실패: 모든 날짜에서 복구 대상 찾지 못함 - from={}, to={}"
          , from.toLocalDate(), to.toLocalDate());
      return List.of();
    }

    List<UUID> allRestoredIds = new ArrayList<>();
    for (var entry : articlesToRestoreByDate.entrySet()) {
      LocalDate date = entry.getKey();
      List<ArticleBackup> backups = entry.getValue();
      UUID restoreJobId = restoreJobIdsByDate.get(date);

      try {
        List<UUID> articleIds = articleBatchService
            .saveRestoredArticlesAndLog(backups, restoreJobId);
        allRestoredIds.addAll(articleIds);
      } catch (Exception e) {
        log.error("{} 날짜 저장 실패", date, e);
        articleBackupJobLogService.recordRestoreFailed(restoreJobId,
            "DB 저장중 오류: " + e.getMessage());
      }
    }

    log.info("뉴스기사 복구 성공 - restoredArticleCount={}, from={}, to={}",
        allRestoredIds.size(), from.toLocalDate(), to.toLocalDate());
    return List.of(
        new ArticleRestoreResultDto(Instant.now(), allRestoredIds, allRestoredIds.size()));
  }

  private ArticleCursor parseCursor(ArticleSearchRequest request) {
    String cursor = request.cursor();
    if (cursor == null || cursor.isEmpty()) {
      return null;
    }

    String[] cursorSplit = cursor.split(CURSOR_DELIMITER);
    if (cursorSplit.length != 2) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
          Map.of("cursor", "Invalid cursor format"));
    }

    try {
      Object value = switch (request.orderBy()) {
        case PUBLISH_DATE -> Instant.parse(cursorSplit[0]);
        case COMMENT_COUNT, VIEW_COUNT -> Integer.valueOf(cursorSplit[0]);
      };
      Instant after = Instant.parse(cursorSplit[1]);

      return new ArticleCursor(value, after);
    } catch (DateTimeParseException | NumberFormatException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, Map.of("cursor", e.getMessage()));
    }
  }

  private NewsArticle findActiveArticleOrElseThrow(UUID articleId) {
    NewsArticle article = newsArticleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));

    // 논리삭제 여부 판단
    if (article.isDeleted()) {
      throw new DeletedArticleException(articleId);
    }

    return article;
  }

  private NewsArticle getArticleOrThrow(UUID articleId) {
    return newsArticleRepository.findById(articleId)
        .orElseThrow(() -> new ArticleNotFoundException(articleId));
  }

  List<ArticleBackup> decompressGzipAndReturnArticlesToRestore(
      InputStream stream, int limit, Set<String> existingLinks) {
    if (limit <= 0) {
      return List.of();
    }

    List<ArticleBackup> articles = new ArrayList<>();
    try (GZIPInputStream gis = new GZIPInputStream(stream);
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(gis, StandardCharsets.UTF_8))) {

      int count = 0;
      String line;
      while ((line = reader.readLine()) != null && count < limit) {
        ArticleBackup article = backupObjectMapper.readValue(line, ArticleBackup.class);
        // 복구된 기사가 기사링크 모음에 없다면 추가
        if (!existingLinks.contains(article.originalLink())) {
          articles.add(article);
          count++;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return articles;
  }
}

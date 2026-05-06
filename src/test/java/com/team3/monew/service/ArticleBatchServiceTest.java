package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.dto.article.ArticleRestoreResultDto;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ArticleBatchServiceTest {

  @Mock
  private NewsArticleRepository newsArticleRepository;
  @Mock
  private NewsSourceRepository newsSourceRepository;
  @Mock
  private ArticleBackupJobLogService articleBackupJobLogService;
  @Mock
  private EntityManager em;

  @InjectMocks
  private ArticleBatchService articleBatchService;

  private Map<NewsSourceType, UUID> sourceTypeIdMap;

  @Test
  @DisplayName("백업된 데이터가 들어오면 정제하고 DB에 저장 및 로그를 기록한다")
  void shouldSaveArticleAndRecordLog_whenBackupDateIsGiven() {
    // given
    UUID naverId = UUID.randomUUID();
    UUID chosunId = UUID.randomUUID();
    sourceTypeIdMap = new HashMap<>();
    sourceTypeIdMap.put(NewsSourceType.NAVER, naverId);
    sourceTypeIdMap.put(NewsSourceType.CHOSUN, chosunId);
    ReflectionTestUtils.setField(articleBatchService, "sourceTypeIdMap", sourceTypeIdMap);

    given(em.getReference(NewsSource.class, naverId)).willReturn(mock(NewsSource.class));
    given(em.getReference(NewsSource.class, chosunId)).willReturn(mock(NewsSource.class));

    Instant time1 = Instant.now();
    Instant time2 = time1.plus(1, ChronoUnit.HOURS);
    ArticleBackup backup1 = new ArticleBackup(
        NewsSourceType.NAVER, "link1", "title1", time1, "summary1");
    ArticleBackup backup2 = new ArticleBackup(
        NewsSourceType.CHOSUN, "link2", "title2", time2, "summary2");
    List<ArticleBackup> backups = List.of(backup1, backup2);
    UUID jobId = UUID.randomUUID();

    UUID articleId1 = UUID.randomUUID();
    UUID articleId2 = UUID.randomUUID();
    given(newsArticleRepository.saveAll(anyList())).willAnswer(invocation -> {
      List<NewsArticle> savedArticles = invocation.getArgument(0);
      ReflectionTestUtils.setField(savedArticles.get(0), "id", articleId1);
      ReflectionTestUtils.setField(savedArticles.get(1), "id", articleId2);
      return savedArticles;
    });

    // when
    ArticleRestoreResultDto result = articleBatchService.saveRestoredArticlesAndLog(backups, jobId);

    // then
    then(articleBackupJobLogService).should()
        .recordRestoreSuccess(jobId, (int) result.restoredArticleCount());
    assertEquals(backups.size(), result.restoredArticleCount());
    assertThat(result.restoredArticleIds())
        .hasSize(2)
        .containsExactly(articleId1, articleId2);
  }
}
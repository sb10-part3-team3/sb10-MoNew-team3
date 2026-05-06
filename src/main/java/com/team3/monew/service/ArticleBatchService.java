package com.team3.monew.service;

import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.dto.article.ArticleRestoreResultDto;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.base.BaseEntity;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleBatchService {

  private final NewsArticleRepository newsArticleRepository;
  private final NewsSourceRepository newsSourceRepository;
  private final ArticleBackupJobLogService articleBackupJobLogService;
  private final EntityManager em;
  private Map<NewsSourceType, UUID> sourceTypeIdMap = new HashMap<>();

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ArticleRestoreResultDto saveRestoredArticlesAndLog(List<ArticleBackup> backups,
      UUID restoreJobId) {

    List<NewsArticle> articles = backups.stream()
        .map(backup -> {
          UUID sourceId = getSourceId(backup.sourceType());
          NewsSource sourceProxy = em.getReference(NewsSource.class, sourceId); // 준영속 문제 해결

          return NewsArticle.create(
              sourceProxy,
              backup.originalLink(),
              backup.title(),
              backup.publishedAt(),
              backup.summary()
          );
        }).toList();
    newsArticleRepository.saveAll(articles);
    Instant finishedAt = articleBackupJobLogService
        .recordRestoreSuccess(restoreJobId, articles.size());
    return new ArticleRestoreResultDto(finishedAt,
        articles.stream().map(BaseEntity::getId).toList(), articles.size());
  }

  private UUID getSourceId(NewsSourceType sourceType) {
    return sourceTypeIdMap.computeIfAbsent(sourceType, type ->
        newsSourceRepository.findByName(sourceType.name())
            .map(NewsSource::getId)
            .orElseThrow(() -> new RuntimeException("지원하지 않는 타입: " + type))
    );
  }
}

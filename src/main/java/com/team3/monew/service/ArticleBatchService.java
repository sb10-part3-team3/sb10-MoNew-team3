package com.team3.monew.service;

import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.dto.article.ArticleRestoreResultDto;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.base.BaseEntity;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private Map<NewsSourceType, UUID> sourceTypeIdMap;

  @PostConstruct
  public void init() {
    this.sourceTypeIdMap = newsSourceRepository.findAll().stream()
        .collect(Collectors.toMap(NewsSource::getSourceType, NewsSource::getId));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ArticleRestoreResultDto saveRestoredArticlesAndLog(List<ArticleBackup> backups,
      UUID restoreJobId) {

    List<NewsArticle> articles = backups.stream()
        .map(backup -> {
          UUID sourceId = sourceTypeIdMap.get(backup.sourceType());
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
}

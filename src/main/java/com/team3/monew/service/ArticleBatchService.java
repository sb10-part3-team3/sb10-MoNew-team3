package com.team3.monew.service;

import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.dto.article.ArticleRestoreResultDto;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.base.BaseEntity;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ArticleRestoreResultDto saveRestoredArticlesAndLog(List<ArticleBackup> backups,
      UUID restoreJobId) {
    Map<NewsSourceType, NewsSource> sourceMap = newsSourceRepository.findAll().stream()
        .collect(Collectors.toMap(NewsSource::getSourceType, source -> source));

    List<NewsArticle> articles = backups.stream()
        .map(backup -> NewsArticle.create(
            sourceMap.get(backup.sourceType()),
            backup.originalLink(),
            backup.title(),
            backup.publishedAt(),
            backup.summary()
        )).toList();
    newsArticleRepository.saveAll(articles);
    Instant finishedAt = articleBackupJobLogService
        .recordRestoreSuccess(restoreJobId, articles.size());
    return new ArticleRestoreResultDto(finishedAt,
        articles.stream().map(BaseEntity::getId).toList(), articles.size());
  }
}

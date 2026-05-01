package com.team3.monew.exception.article;

import java.util.UUID;

public class ArticleBackupJobNotFoundException extends RuntimeException {

  public ArticleBackupJobNotFoundException(UUID backupJobId) {
    super("존재하지 않는 backupJobId입니다: " + backupJobId);
  }
}

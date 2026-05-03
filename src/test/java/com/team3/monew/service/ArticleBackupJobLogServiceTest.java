package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.team3.monew.entity.ArticleBackupJob;
import com.team3.monew.entity.enums.BackupJobStatus;
import com.team3.monew.entity.enums.BackupJobType;
import com.team3.monew.exception.article.ArticleBackupJobNotFoundException;
import com.team3.monew.repository.ArticleBackupJobRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ArticleBackupJobLogServiceTest {

  @Mock
  private ArticleBackupJobRepository articleBackupJobRepository;

  @InjectMocks
  private ArticleBackupJobLogService articleBackupJobLogService;


  @Test
  @DisplayName("주입받은 날짜로 backupJob객체를 저장합니다")
  void shouldSaveBackupJob_whenDateGiven() {
    // given
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    String bucket = "bucket";
    String key = "key";

    // when
    articleBackupJobLogService.createBackupJob(today, bucket, key);

    // then
    ArgumentCaptor<ArticleBackupJob> captor = ArgumentCaptor.forClass(ArticleBackupJob.class);
    then(articleBackupJobRepository).should().save(captor.capture());
    ArticleBackupJob backupJob = captor.getValue();
    assertThat(backupJob.getJobType()).isEqualTo(BackupJobType.ARTICLE_DAILY_BACKUP);
    assertThat(backupJob.getBackupDate()).isEqualTo(today);
    assertThat(backupJob.getS3Bucket()).isEqualTo(bucket);
    assertThat(backupJob.getS3Key()).isEqualTo(key);
  }

  @Test
  @DisplayName("백업 성공시 백업한 기사수를 설정하고 상태를 success로 변경한다")
  void shouldUpdateStatusToSuccessAndRecordArticleCount_whenBackupSucceeds() {
    // given
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    int articleCount = 1740;
    ArticleBackupJob backupJob = ArticleBackupJob.create(today, BackupJobType.ARTICLE_DAILY_BACKUP)
        .setArticleCount(articleCount);
    given(articleBackupJobRepository.findById(any(UUID.class))).willReturn(Optional.of(backupJob));

    // when
    articleBackupJobLogService.recordSuccess(UUID.randomUUID(), articleCount);

    // then
    ArgumentCaptor<ArticleBackupJob> captor = ArgumentCaptor.forClass(ArticleBackupJob.class);
    then(articleBackupJobRepository).should().save(captor.capture());
    ArticleBackupJob capturedBackupJob = captor.getValue();
    assertThat(capturedBackupJob.getStatus()).isEqualTo(BackupJobStatus.SUCCESS);
    assertThat(capturedBackupJob.getArticleCount()).isEqualTo(articleCount);
  }

  @Test
  @DisplayName("백업 실패시 실패한 메세지를 입력하고 상태를 failed로 기록한다")
  void shouldUpdateStatusToFailedAndRecordErrorMessage_whenBackupFails() {
    // given
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    String errorMessage = "에러 메세지";
    ArticleBackupJob backupJob = ArticleBackupJob.create(today, BackupJobType.ARTICLE_DAILY_BACKUP)
        .setErrorMessage(errorMessage);
    given(articleBackupJobRepository.findById(any(UUID.class))).willReturn(Optional.of(backupJob));

    // when
    articleBackupJobLogService.recordFailed(UUID.randomUUID(), errorMessage);

    // then
    ArgumentCaptor<ArticleBackupJob> captor = ArgumentCaptor.forClass(ArticleBackupJob.class);
    then(articleBackupJobRepository).should().save(captor.capture());
    ArticleBackupJob capturedBackupJob = captor.getValue();
    assertThat(capturedBackupJob.getStatus()).isEqualTo(BackupJobStatus.FAILED);
    assertThat(capturedBackupJob.getErrorMessage()).isEqualTo(errorMessage);
  }

  @Test
  @DisplayName("존재하지 않는 backupJob을 불러오면 예외를 반환한다")
  void shouldThrowException_whenBackupJobDoesNotExist() {
    // given
    UUID backupJobId = UUID.randomUUID();
    given(articleBackupJobRepository.findById(any(UUID.class))).willReturn(Optional.empty());

    // when & then
    assertThrows(ArticleBackupJobNotFoundException.class,
        () -> articleBackupJobLogService.recordSuccess(backupJobId, 123));
  }
}
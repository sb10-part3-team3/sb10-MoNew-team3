package com.team3.monew.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.config.AwsProperties;
import com.team3.monew.dto.article.ArticleBackup;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.NewsArticleRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleBackupService {

  private final AwsProperties awsProperties;
  private final S3Client s3Client;
  private final NewsArticleRepository newsArticleRepository;
  private final ArticleMapper articleMapper;
  private final ArticleBackupJobLogService articleBackupJobLogService;
  private final ObjectMapper objectMapper;

  private static final String BACKUP_URI = "test/csj";
  private static final DateTimeFormatter S3_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

  @Transactional
  public void backupYesterdayArticle() {
    // 뉴스기사 수집: UTC
    // 백업기준 한국: Asia/Seoul
    ZoneId zone = ZoneId.of("Asia/Seoul");
    LocalDate today = LocalDate.now(zone);
    LocalDate yesterday = today.minusDays(1);
    Instant startOfToday = today.atStartOfDay(zone).toInstant();
    Instant startOfYesterday = startOfToday.minus(1, ChronoUnit.DAYS);
    log.debug("뉴스기사 백업 시작 - backupTargetDate={}", yesterday);

    // Key
    String backupFilename = "backup_" + yesterday + ".json.gz";
    String key = generateS3Key(yesterday, backupFilename);  // 경로/yyyy/MM/backUp_yyyy-MM-dd.gz

    // BackUpJob
    UUID backupJobId = articleBackupJobLogService.createBackupJob(yesterday,
        awsProperties.getS3().getBucket(), key);

    // 어제 자정부터 오늘 자정 전까지(발행일 기준)
    List<ArticleBackup> backupList = newsArticleRepository
        .findAllByPeriod(startOfYesterday, startOfToday).stream()
        .map(articleMapper::toBackupDto).toList();

    try {
      // List -> Json
      String json = objectMapper.writeValueAsString(backupList);

      // 파일 압축
      byte[] compressedData = null;
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
          GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
        gzos.write(json.getBytes(StandardCharsets.UTF_8));
        gzos.finish();    // 압축 끝 명시
        compressedData = baos.toByteArray();
      }

      PutObjectResponse response = s3Client.putObject(
          req -> req.bucket(awsProperties.getS3().getBucket())
              .key(key)
              .contentEncoding("gzip")
              .contentType("application/json"),
          RequestBody.fromBytes(compressedData));

      if (response.sdkHttpResponse().isSuccessful()) {
        log.info("뉴스기사 백업 성공 - backupTargetDate={}, articleCount={}", yesterday, backupList.size());
        articleBackupJobLogService.recordSuccess(backupJobId, backupList.size());
        return;
      }
      int statusCode = response.sdkHttpResponse().statusCode();
      log.error("뉴스기사 백업 실패 - backupTargetDate={}, statusCode={}", yesterday, statusCode);
      String errorMessage = "S3 응답실패 코드: " + statusCode;
      articleBackupJobLogService.recordFailed(backupJobId, errorMessage);
    } catch (Exception e) {
      String summary = "기타 시스템 에러";
      if (e instanceof JsonProcessingException) {
        summary = "Json 변환 실패";
      } else if (e instanceof IOException) {
        summary = "압축 처리 실패";
      } else if (e instanceof S3Exception) {
        summary = "AWS S3 전송 실패";
      }

      log.error("{} - backupTargetDate={}, error={}", summary, yesterday, e.getMessage());
      String errorMessage = String.format("[%s] %s", e.getClass().getSimpleName(), summary);
      articleBackupJobLogService.recordFailed(backupJobId, errorMessage);
    }
  }

  private String generateS3Key(LocalDate localDate, String fileName) {
    return BACKUP_URI + "/" + localDate.format(S3_PATH_FORMATTER) + "/" + fileName;
  }
}

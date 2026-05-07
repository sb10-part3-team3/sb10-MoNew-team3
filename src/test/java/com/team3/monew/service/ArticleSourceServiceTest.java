package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.mapper.ArticleMapper;
import com.team3.monew.repository.ArticleBackupJobRepository;
import com.team3.monew.repository.ArticleInterestRepository;
import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ArticleSourceServiceTest {

  @Spy
  private ArticleMapper articleMapper = Mappers.getMapper(ArticleMapper.class);

  @Mock
  private NewsArticleRepository newsArticleRepository;
  @Mock
  private ArticleViewRepository articleViewRepository;
  @Mock
  private ArticleViewService articleViewService;
  @Mock
  private ArticleInterestRepository articleInterestRepository;
  @Mock
  private CommentRepository commentRepository;
  @Mock
  private NewsSourceRepository newsSourceRepository;
  @Mock
  private ArticleBackupJobRepository articleBackupJobRepository;
  @Mock
  private ArticleBatchService articleBatchService;
  @Mock
  private S3AsyncClient s3AsyncClient;
  @Mock
  private TaskExecutor decompressTaskExecutor;
  @Mock
  private ArticleBackupJobLogService articleBackupJobLogService;
  @Mock
  private ObjectMapper backupObjectMapper;

  @InjectMocks
  private ArticleService articleService;

  @Nested
  @DisplayName("뉴스 기사 출처 목록을 조회한다")
  class GetArticleSources {

    @Test
    @DisplayName("등록된 출처가 있으면 중복 없이 문자열 목록을 반환한다")
    void shouldReturnDistinctArticleSources_whenNewsSourcesExist() {
      NewsSource naverPrimary = NewsSource.create("NAVER", NewsSourceType.NAVER,
          "https://openapi.naver.com");
      NewsSource naverSecondary = NewsSource.create("NAVER-SECONDARY", NewsSourceType.NAVER,
          "https://openapi.naver.com/news");
      NewsSource chosun = NewsSource.create("CHOSUN", NewsSourceType.CHOSUN,
          "https://www.chosun.com");

      given(newsSourceRepository.findAll()).willReturn(List.of(naverPrimary, naverSecondary, chosun));

      List<String> actual = articleService.getArticleSources();

      assertThat(actual).containsExactly("NAVER", "CHOSUN");
    }

    @Test
    @DisplayName("등록된 출처가 없으면 빈 목록을 반환한다")
    void shouldReturnEmptyList_whenNewsSourcesDoNotExist() {
      given(newsSourceRepository.findAll()).willReturn(List.of());

      List<String> actual = articleService.getArticleSources();

      assertThat(actual).isEmpty();
    }
  }
}

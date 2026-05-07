package com.team3.monew.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.team3.monew.component.news.client.ChosunNewsClient;
import com.team3.monew.component.news.client.NaverNewsClient;
import com.team3.monew.component.news.client.NewsClient;
import com.team3.monew.component.news.filter.KeywordMatch;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.InterestKeywordRepository;
import com.team3.monew.service.NewsSaveService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ArticleCollectBatchConfigTest {

  @Nested
  class InterestKeywordReader {

    @Mock
    private InterestKeywordRepository interestKeywordRepository;
    @Mock
    private KeywordMatch keywordMatch;

    @InjectMocks
    private ArticleCollectBatchConfig.InterestKeywordReader interestKeywordReader;

    @Test
    @DisplayName("repository에서 얻은 List로 한번에 반환한다")
    void shouldReturnKeywordList_whenReadFirstTime() {
      // given
      Interest samsungInterest = Interest.create("삼성");
      InterestKeyword interestKeyword = InterestKeyword.create(samsungInterest, "메모리");
      List<InterestKeyword> expected = List.of(interestKeyword);
      given(interestKeywordRepository.findAllWithInterest()).willReturn(expected);

      // when
      List<InterestKeyword> actual = interestKeywordReader.read();

      // then
      then(keywordMatch).should().refreshKeywords(anySet());
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("reader를 두 번 호출하면 null을 반환한다")
    void shouldReturnNull_whenReadCalledSecondTime() {
      // when
      interestKeywordReader.read();
      List<InterestKeyword> actual = interestKeywordReader.read();

      // then
      assertThat(actual).isNull();
    }
  }

  @Nested
  class NewsCollectProcessor {

    @Mock
    private NaverNewsClient naverNewsClient;
    @Mock
    private ChosunNewsClient chosunNewsClient;
    @Mock
    private Map<String, NewsClient> newsClients;

    @InjectMocks
    private ArticleCollectBatchConfig.NewsCollectProcessor newsCollectProcessor;

    @Test
    @DisplayName("동일한 링크 기사가 여러 Source에서 수집되면, 네이버 기사를 최우선으로 남긴다")
    void shouldKeepNaverArticle_whenMultipleSourcesProvideSameLink() {
      // given
      Interest samsungInterest = Interest.create("삼성");
      List<InterestKeyword> interestKeywords = List.of(
          InterestKeyword.create(samsungInterest, "메모리"));
      String commonLink = "commonLink";
      given(newsClients.values()).willReturn(List.of(naverNewsClient, chosunNewsClient));
      given(newsClients.size()).willReturn(2);

      List<ParsedNewsArticle> naverList = List.of(
          new ParsedNewsArticle(NewsSourceType.NAVER, commonLink, "제목1", Instant.now(), null,
              interestKeywords)
      );
      given(naverNewsClient.fetchAndProcess(anyList())).willReturn(Mono.just(naverList));
      List<ParsedNewsArticle> chosunList = List.of(
          new ParsedNewsArticle(NewsSourceType.CHOSUN, commonLink, "제목1", Instant.now(), null,
              interestKeywords)
      );
      given(chosunNewsClient.fetchAndProcess(anyList())).willReturn(Mono.just(chosunList));

      // when
      List<ParsedNewsArticle> actual = newsCollectProcessor.process(interestKeywords);

      // then
      assertThat(actual)
          .extracting("sourceType")
          .containsExactly(NewsSourceType.NAVER);
    }
  }

  @Nested
  class NewsArticleWriter {

    @Mock
    private NewsSaveService newsSaveService;
    @Mock
    private StepExecution stepExecution;
    @Mock
    private JobExecution jobExecution;
    @Mock
    private ExecutionContext executionContext;

    @InjectMocks
    private ArticleCollectBatchConfig.NewsArticleWriter newsArticleWriter;

    @Test
    @DisplayName("기사를 저장하고 저장 개수를 execution context에 기록한다")
    void shouldSaveArticlesAndStoreCount_whenWriteCalled() {
      // given
      ParsedNewsArticle parsed = new ParsedNewsArticle(NewsSourceType.NAVER, "link", "title",
          Instant.now(), "summary", List.of());
      List<ParsedNewsArticle> parsedNewsArticles = List.of(parsed);

      NewsArticle article = NewsArticle.create(mock(NewsSource.class), "link", "title",
          Instant.now(), "summary");
      List<NewsArticle> articles = List.of(article);
      given(newsSaveService.saveAndNotify(anyList())).willReturn(articles);

      ReflectionTestUtils.setField(newsArticleWriter, "stepExecution", stepExecution);
      given(stepExecution.getJobExecution()).willReturn(jobExecution);
      given(jobExecution.getExecutionContext()).willReturn(executionContext);

      // when
      newsArticleWriter.write(new Chunk<>(List.of(parsedNewsArticles)));

      // then
      then(executionContext).should()
          .putLong("savedArticleCount", 1L);
    }

  }
}

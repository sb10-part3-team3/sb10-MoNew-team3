package com.team3.monew.batch.job;

import com.team3.monew.component.news.client.NewsClient;
import com.team3.monew.component.news.filter.KeywordMatch;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.InterestKeywordRepository;
import com.team3.monew.service.NewsSaveService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import reactor.core.publisher.Flux;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ArticleCollectBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager platformTransactionManager;

  private final KeywordMatch keywordMatch;
  private final InterestKeywordRepository interestKeywordRepository;
  private final NewsSaveService newsSaveService;
  private final Map<String, NewsClient> newsClients;

  @Bean
  public Job articleCollectBatchJob() {
    return new JobBuilder("articleCollectBatchJob", jobRepository)
        .start(collectArticleStep())
        .build();
  }

  @Bean
  public Step collectArticleStep() {
    return new StepBuilder("collectArticleStep", jobRepository)
        .<List<InterestKeyword>, List<ParsedNewsArticle>>chunk(1, platformTransactionManager)
        .reader(interestKeywordReader())
        .processor(newsCollectProcessor())
        .writer(newsArticleWriter())
        .build();
  }

  @Bean
  @StepScope
  public ItemReader<List<InterestKeyword>> interestKeywordReader() {
    return new InterestKeywordReader(interestKeywordRepository, keywordMatch);
  }

  @RequiredArgsConstructor
  static class InterestKeywordReader implements ItemReader<List<InterestKeyword>> {

    private final InterestKeywordRepository interestKeywordRepository;
    private final KeywordMatch keywordMatch;
    private boolean alreadyRead = false;

    @Override
    public List<InterestKeyword> read() {
      if (alreadyRead) {
        return null;
      }
      alreadyRead = true;

      List<InterestKeyword> interestKeywordList = interestKeywordRepository.findAllWithInterest();

      // 관심사와, 키워드를 하나로 묶기
      Set<String> keywords = interestKeywordList.stream()
          .flatMap(ik -> Stream.of(ik.getInterest().getName(), ik.getKeyword()))
          .collect(Collectors.toSet());
      keywordMatch.refreshKeywords(keywords);

      return interestKeywordList;
    }
  }

  @Bean
  public ItemProcessor<List<InterestKeyword>, List<ParsedNewsArticle>> newsCollectProcessor() {
    return new NewsCollectProcessor(newsClients);
  }

  @RequiredArgsConstructor
  static class NewsCollectProcessor implements
      ItemProcessor<List<InterestKeyword>, List<ParsedNewsArticle>> {

    private final Map<String, NewsClient> newsClients;

    @Override
    public List<ParsedNewsArticle> process(List<InterestKeyword> interestKeywords) {

      return Flux.fromIterable(newsClients.values())
          .flatMap(
              client -> client.fetchAndProcess(interestKeywords),
              newsClients.size()
          )
          .flatMapIterable(list -> list)
          .collectList()
          .map(this::deduplicateByLinkWithNaverPriorityAndOrderByPublishAtDesc)
          .block();
    }

    private List<ParsedNewsArticle> deduplicateByLinkWithNaverPriorityAndOrderByPublishAtDesc(
        List<ParsedNewsArticle> list) {
      return list.stream()
          .collect(Collectors.toMap(
              ParsedNewsArticle::link,               //   key: link
              article -> article,     // value: ParsedNewsArticle
              (existing, replacement) ->
                  // merge function. 중복일 시 Naver우선으로 충돌 해결
                  NewsSourceType.NAVER.equals(existing.sourceType())
                      ? existing
                      : replacement
          ))
          .values()
          .stream()
          .sorted(Comparator.comparing(ParsedNewsArticle::publishedAt).reversed())
          .toList();
    }
  }

  @Bean
  public ItemWriter<List<ParsedNewsArticle>> newsArticleWriter() {
    return new NewsArticleWriter(newsSaveService);
  }

  @RequiredArgsConstructor
  static class NewsArticleWriter implements ItemWriter<List<ParsedNewsArticle>> {

    private final NewsSaveService newsSaveService;
    private StepExecution stepExecution;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
      this.stepExecution = stepExecution;
    }

    @Override
    public void write(Chunk<? extends List<ParsedNewsArticle>> chunk) {
      for (List<ParsedNewsArticle> parsedNewsArticles : chunk) {
        List<NewsArticle> articles = newsSaveService.saveAndNotify(parsedNewsArticles);

        stepExecution.getJobExecution().getExecutionContext()
            .putLong("savedArticleCount", articles.size());
      }
    }
  }
}

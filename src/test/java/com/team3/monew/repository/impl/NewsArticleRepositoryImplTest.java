package com.team3.monew.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.team3.monew.config.JpaAuditingConfig;
import com.team3.monew.config.QueryDslConfig;
import com.team3.monew.dto.article.internal.ArticleCursor;
import com.team3.monew.dto.article.internal.ArticleSearchCondition;
import com.team3.monew.dto.article.internal.enums.ArticleDirection;
import com.team3.monew.dto.article.internal.enums.ArticleOrderBy;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({NewsArticleRepositoryImpl.class, JpaAuditingConfig.class, QueryDslConfig.class})
@Tag("integration")
@ActiveProfiles("test")
class NewsArticleRepositoryImplTest {

  @Autowired
  private NewsArticleRepository newsArticleRepository;
  @Autowired
  private NewsSourceRepository newsSourceRepository;
  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private TestEntityManager em;

  private ArticleSearchCondition cond;
  private NewsSource naverSource;
  private NewsSource chosunSource;
  private NewsArticle article1;
  private NewsArticle article2;
  private NewsArticle article3;
  private Interest samsungInterest;
  private Interest appleInterest;

  private Instant time;

  @BeforeEach
  void setUp() {
    newsArticleRepository.deleteAll();
    interestRepository.deleteAll();
    newsSourceRepository.deleteAll();

    naverSource = NewsSource.create(NewsSourceType.NAVER.name(), NewsSourceType.NAVER, "baseUrl..");
    chosunSource = NewsSource.create(NewsSourceType.CHOSUN.name(), NewsSourceType.CHOSUN,
        "baseUrl..");
    newsSourceRepository.saveAll(List.of(naverSource, chosunSource));

    samsungInterest = Interest.create("삼성");
    samsungInterest.addKeyword("메모리");
    samsungInterest.addKeyword("갤럭시");
    appleInterest = Interest.create("애플");
    appleInterest.addKeyword("아이폰");
    interestRepository.saveAll(List.of(samsungInterest, appleInterest));

    article1 = NewsArticle.create(naverSource, "link1", "제목1 아이폰 애플",
        Instant.now().minus(10, ChronoUnit.DAYS), "요약1 갤럭시 삼성");
    article2 = NewsArticle.create(naverSource, "link2", "제목2 HBM Nvidia 삼성",
        Instant.now().minus(20, ChronoUnit.DAYS), "요약2 배터리 메모리");
    article3 = NewsArticle.create(chosunSource, "link3", "제목3 화약 애플",
        Instant.now().minus(30, ChronoUnit.DAYS), "요약3 HBM메모리 삼성");

    article1.addArticleInterest(samsungInterest, "갤럭시");
    article1.addArticleInterest(appleInterest, "아이폰");
    article2.addArticleInterest(samsungInterest, "메모리");
    article3.addArticleInterest(samsungInterest, "메모리");
    newsArticleRepository.saveAll(List.of(article1, article2, article3));
    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("조건식에 맞는 기사들의 갯수를 반환한다")
  void shouldReturnCount_whenSearchCondition() {
    // given
    ArticleSearchCondition cond = new ArticleSearchCondition(
        null, null, List.of(NewsSourceType.NAVER, NewsSourceType.CHOSUN),
        Instant.now().minus(50, ChronoUnit.DAYS),
        Instant.now().minus(15, ChronoUnit.DAYS),
        ArticleOrderBy.PUBLISH_DATE, ArticleDirection.DESC, null, 10);

    // when
    Long actual = newsArticleRepository.countByCondition(cond);

    // then
    assertThat(actual).isEqualTo(2);
  }

  @Test
  @DisplayName("키워드로 제목이나 요약이 매칭되면 기사들을 반환한다")
  void shouldReturnArticles_whenKeywordMatchesTitleOrSummary() {
    // given
    String keyword = "HBM";
    ArticleSearchCondition cond = new ArticleSearchCondition(
        keyword, null, List.of(NewsSourceType.NAVER, NewsSourceType.CHOSUN),
        null, null, ArticleOrderBy.COMMENT_COUNT, ArticleDirection.ASC,
        null, 10);

    // when
    List<NewsArticle> articles = newsArticleRepository.searchByCondition(cond);

    // then
    assertThat(articles)
        .hasSize(2)
        .allMatch(a -> a.getTitle().contains(keyword) || a.getSummary().contains(keyword));
    assertThat(articles)
        .extracting(NewsArticle::getTitle)
        .anyMatch(s -> s.contains("제목2 HBM Nvidia"));
    assertThat(articles)
        .extracting(NewsArticle::getSummary)
        .anyMatch(s -> s.contains("요약3 HBM메모리"));
  }

  @Test
  @DisplayName("관심사에 해당하는 기사들을 반환한다")
  void shouldReturnArticles_whenSearchByInterest() {
    // given
    ArticleSearchCondition cond = new ArticleSearchCondition(
        null, appleInterest.getId(), List.of(NewsSourceType.NAVER, NewsSourceType.CHOSUN),
        null, null, ArticleOrderBy.VIEW_COUNT, ArticleDirection.ASC,
        null, 10);

    // when
    List<NewsArticle> articles = newsArticleRepository.searchByCondition(cond);

    assertThat(articles)
        .hasSize(1)
        .first()
        .extracting(NewsArticle::getTitle)
        .matches(a -> a.contains("애플") || a.contains("아이폰"));

  }

  @Test
  @DisplayName("cursor 조건에 해당하는 기사들을 반환한다")
  void shouldReturnArticles_whenSearchByCondition() {
    // given
    time = Instant.now();
    ArticleCursor cursor = new ArticleCursor(5, time);
    ArticleSearchCondition cond = new ArticleSearchCondition(
        "제목", null, List.of(NewsSourceType.NAVER, NewsSourceType.CHOSUN),
        null, null, ArticleOrderBy.VIEW_COUNT, ArticleDirection.DESC,
        cursor, 5);
    // 엔티티 값 다시 설정
    NewsArticle a1 = newsArticleRepository.findById(article1.getId()).orElseThrow();
    NewsArticle a2 = newsArticleRepository.findById(article2.getId()).orElseThrow();
    NewsArticle a3 = newsArticleRepository.findById(article3.getId()).orElseThrow();
    ReflectionTestUtils.setField(a1, "viewCount", 6);
    ReflectionTestUtils.setField(a2, "viewCount", 5);
    ReflectionTestUtils.setField(a3, "viewCount", 4);
    newsArticleRepository.saveAll(List.of(a1, a2, a3));
    em.flush();
    em.clear();

    // when
    List<NewsArticle> articles = newsArticleRepository.searchByCondition(cond);

    // then
    assertThat(articles)
        .filteredOn(na -> na.getViewCount() <= 5)
        .extracting("viewCount", "originalLink")
        .containsExactly(
            tuple(5, "link2"),
            tuple(4, "link3")
        );
  }

}
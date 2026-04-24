package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.ArticleInterestRepository;
import com.team3.monew.repository.InterestKeywordRepository;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.service.NewsCollectService;
import com.team3.monew.support.IntegrationTestSupport;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
@Tag("external-api")
@TestPropertySource(locations = "file:.env")
class NewsCollectIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private InterestKeywordRepository interestKeywordRepository;

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  @Autowired
  private NewsCollectService newsCollectService;

  @Autowired
  private ArticleInterestRepository articleInterestRepository;

  private Set<String> keywords;

  @BeforeEach
  void setUp() {
    Interest samsung = Interest.create("삼성");
    interestRepository.save(samsung);
    InterestKeyword keyword = InterestKeyword.create(samsung, "메모리");
    InterestKeyword keyword2 = InterestKeyword.create(samsung, "갤럭시");

    Interest apple = Interest.create("애플");
    interestRepository.save(apple);
    InterestKeyword keyword3 = InterestKeyword.create(apple, "아이폰");

    List<InterestKeyword> interestKeywordList = List.of(keyword, keyword2, keyword3);
    interestKeywordRepository.saveAll(interestKeywordList);

    keywords = interestKeywordList.stream()
        .map(InterestKeyword::getKeyword).collect(Collectors.toSet());
  }

  @AfterEach
  void tearDown() {
    articleInterestRepository.deleteAll();
    newsArticleRepository.deleteAll();
    interestKeywordRepository.deleteAll();
    interestRepository.deleteAll();
  }

  @Test
  @DisplayName("뉴스 수집이 실행되면 기사들을 수집하고 저장한다")
  void shouldCollectAndSaveArticles_whenNewsCollectionIsExecuted() {
    // when
    // NewsSource는 현재 Naver, Chosun 2개 존재
    // 네이버 요청시 쿼리당 100개
    StepVerifier.create(newsCollectService.executeNewsCollection())
        .verifyComplete();

    // then
    List<NewsArticle> articles = newsArticleRepository.findAllWithNewsSource();
    assertThat(articles)
        .isNotEmpty()
        .filteredOn(na -> na.getSource().getSourceType() == NewsSourceType.NAVER)
        .hasSizeGreaterThanOrEqualTo(100)
        .allSatisfy(article -> {
          boolean inTitle = keywords.stream()
              .anyMatch(keyword -> article.getTitle().contains(keyword));
          boolean inSummary = keywords.stream()
              .anyMatch(keyword -> article.getSummary().contains(keyword));

          assertThat(inTitle || inSummary)
              .as("기사의 제목이나 내용 중에 해당하는 키워드가 없습니다")
              .isTrue();
        });

    assertThat(articles)
        // 네이버 쿼리는 키워드 형태로 요청해서 100개 이상인걸 보장
        .hasSizeGreaterThanOrEqualTo(100);
  }
}

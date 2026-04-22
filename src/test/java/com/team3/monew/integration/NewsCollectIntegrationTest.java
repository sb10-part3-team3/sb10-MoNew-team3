package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.InterestKeywordRepository;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.service.NewsCollectService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import reactor.test.StepVerifier;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
@Tag("external-api")
@TestPropertySource(locations = "file:.env")
class NewsCollectIntegrationTest {

  @Autowired
  private InterestKeywordRepository interestKeywordRepository;

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private NewsArticleRepository newsArticleRepository;

  @Autowired
  private NewsCollectService newsCollectService;

  @Test
  @DisplayName("뉴스 수집이 실행되면 기사들을 수집하고 저장한다")
  void shouldCollectAndSaveArticles_whenNewsCollectionIsExecuted() {
    // given
    Interest samsung = Interest.create("삼성");
    interestRepository.save(samsung);
    InterestKeyword keyword = InterestKeyword.create(samsung, "메모리");
    interestKeywordRepository.save(keyword);
    // NewsSource는 SpringBoot 시에 저장하는 것으로 사용

    // when
    // NewsSource는 현재 Naver, Chosun 2개 존재
    // 네이버 요청시에 100개
    StepVerifier.create(newsCollectService.executeNewsCollection())
        .verifyComplete();

    // then
    List<NewsArticle> articles = newsArticleRepository.findAll();
    assertThat(articles)
        .isNotEmpty()
        .hasSizeGreaterThanOrEqualTo(100)
        .allSatisfy(article -> {
          boolean inTitle = article.getTitle().contains("메모리");
          boolean inSummary = article.getSummary().contains("메모리");

          assertThat(inTitle || inSummary)
              .as("기사의 제목이나 내용 중에 메모리 키워드가 없습니다")
              .isTrue();
        });

    assertThat(articles)
        .filteredOn(a -> a.getSource().getSourceType() == NewsSourceType.NAVER)
        // 네이버 쿼리는 키워드 형태로 요청해서 100개 이상인걸 보장
        .hasSizeGreaterThanOrEqualTo(100);

    assertThat(articles)
        .filteredOn(a -> a.getSource().getSourceType() == NewsSourceType.CHOSUN)
        // 조선일보는 쿼리 요청후 키워드를 매칭해서 0개 이상임
        .hasSizeGreaterThanOrEqualTo(0);
  }
}

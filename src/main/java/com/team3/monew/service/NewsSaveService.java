package com.team3.monew.service;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.ArticleInterest;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.base.BaseEntity;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.event.InterestNotificationEvent;
import com.team3.monew.event.InterestNotificationEvent.InterestArticleSummary;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSaveService {

  private final ApplicationEventPublisher eventPublisher;
  private final NewsArticleRepository newsArticleRepository;
  private final NewsSourceRepository newsSourceRepository;
  private final InterestRepository interestRepository;

  @Transactional
  public List<NewsArticle> saveAndNotify(List<ParsedNewsArticle> data) {

    log.debug("뉴스 기사 저장 시작");
    if (data.isEmpty()) {
      log.info("뉴스 기사 저장 스킵 - 저장할 데이터가 없습니다. articleSize=0");
      return List.of();
    }

    // 새로운 뉴스 Link
    List<String> links = data.stream().map(ParsedNewsArticle::link).toList();
    // repository에 이미 저장된 뉴스 Link
    Set<String> existingLinks = newsArticleRepository.findExistingOriginalLinks(
        data.get(data.size() - 1).publishedAt(), links);    // data는 발행일자 역순정렬됨

    // 중복이 걸러진 뉴스기사
    List<ParsedNewsArticle> newNewsArticles = data.stream()
        .filter(raw -> !existingLinks.contains(raw.link()))
        .toList();

    if (newNewsArticles.isEmpty()) {
      log.info("뉴스 기사 저장 스킵 - 저장할 데이터가 없습니다. articleSize=0");
      return List.of();
    }

    // NewsSource 타입
    Map<NewsSourceType, NewsSource> sourceMap = newsSourceRepository.findAll().stream()
        .collect(Collectors.toMap(NewsSource::getSourceType, s -> s));

    // 양방향 편의 메소드 저장을 위해 불러오는 Interest
    Map<String, Interest> interestMap = interestRepository.findAll().stream()
        .collect(Collectors.toMap(Interest::getName, i -> i));

    // Repository에 저장하기 위한 데이터 변환
    List<NewsArticle> savedArticles = newNewsArticles.stream()
        .map(parsed -> {
          // NewsArticle 생성
          NewsArticle article = NewsArticle.create(sourceMap.get(parsed.sourceType()),
              parsed.link(), parsed.title(), parsed.publishedAt(), parsed.summary());

          // ArticleInterest 생성
          parsed.interestKeywords().forEach(ik -> {
            Interest interest = interestMap.get(ik.getInterest().getName());
            if (interest != null) {
              article.addArticleInterest(interest, ik.getKeyword());
            }
          });

          return article;
        })
        .toList();
    // Repository에 데이터 저장
    newsArticleRepository.saveAll(savedArticles);
    log.info("뉴스 기사 저장 완료 - articleSize={}", savedArticles.size());

    // 기사별 관심사
    Map<String, Set<Interest>> InterestsByArticle = savedArticles.stream()
        .flatMap(article -> article.getArticleInterests().stream())
        .collect(Collectors.groupingBy(
            ai -> ai.getArticle().getOriginalLink(),
            mapping(ArticleInterest::getInterest, toSet())
        ));

    // 관심사별 기사 갯수
    Map<Interest, Integer> articlesByInterest = savedArticles.stream()
        .flatMap(article -> article.getArticleInterests().stream())
        .collect(Collectors.groupingBy(
            ArticleInterest::getInterest,
            Collectors.collectingAndThen(
                Collectors.mapping(ArticleInterest::getArticle, toSet()), Set::size
            )
        ));

    // 관심사ID, (관심사 이름, 기사 갯수)
    Map<UUID, InterestArticleSummary> interestArticleSummaryMap = articlesByInterest.keySet()
        .stream()
        .collect(Collectors.toMap(
            BaseEntity::getId,
            i -> new InterestArticleSummary(i.getName(), articlesByInterest.get(i))
        ));

    eventPublisher.publishEvent(new InterestNotificationEvent(interestArticleSummaryMap));

    // 비동기 스트리밍 처리때문에 반환값이 필요하여 그냥 아무값이나 보냅니다
    return savedArticles;
  }
}

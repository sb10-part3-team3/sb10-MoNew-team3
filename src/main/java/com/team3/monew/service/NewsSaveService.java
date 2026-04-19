package com.team3.monew.service;

import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import com.team3.monew.service.NewsParseService.ParsedNewsArticle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSaveService {

  private final NewsArticleRepository newsArticleRepository;
  private final NewsSourceRepository newsSourceRepository;

  @Transactional
  public List<ParsedNewsArticle> save(List<ParsedNewsArticle> data) {

    // 새로운 뉴스 Link
    List<String> newNewsArticleLinks = data.stream()
        .map(ParsedNewsArticle::link)
        .toList();

    // repository에 이미 저장된 뉴스 Link
    Set<String> existingLinks = newsArticleRepository.findAllByOriginalLinkIn(newNewsArticleLinks)
        .stream()
        .map(NewsArticle::getOriginalLink)
        .collect(Collectors.toSet());

    // 중복이 걸러진 뉴스기사
    List<ParsedNewsArticle> newNewsArticles = data.stream()
        .filter(raw -> !existingLinks.contains(raw.link()))
        .toList();

    // NewsSource 타입
    Map<NewsSourceType, NewsSource> newsSources = newsSourceRepository.findAll().stream()
        .collect(Collectors.toMap(
            source -> NewsSourceType.valueOf(source.getName()),
            source -> source
        ));

    // Repository에 저장하기 위한 데이터 변환
    List<NewsArticle> articleToSave = newNewsArticles.stream()
        .map(raw ->
            NewsArticle.create(newsSources.get(raw.sourceType()),
                raw.link(), raw.title(), raw.publishedAt(), raw.summary()))
        .toList();

    // Repository에 데이터 저장
    newsArticleRepository.saveAll(articleToSave);

    // 저장된 뉴스의 원본 반환
    return newNewsArticles;
  }
}

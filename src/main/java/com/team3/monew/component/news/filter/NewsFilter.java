package com.team3.monew.component.news.filter;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsFilter {

  private final KeywordMatch keywordMatch;

  public List<ParsedNewsArticle> filterKeyword(ParsedData parsedData) {
    List<ParsedNewsArticle> parsedNewsArticles = parsedData.articles().stream()
        .map(article -> {
          Set<String> matchedKeywords = Stream.of(
                  keywordMatch.findMatches(article.title()),
                  keywordMatch.findMatches(article.summary()))
              .flatMap(List::stream)
              .collect(Collectors.toSet());

          // set에 데이터가 있어서 article에 저장되면 true
          article.keywords().addAll(matchedKeywords);
          return article;
        })
        .filter(article -> !article.keywords().isEmpty())
        .toList();

    log.info("{} 기사 filter 처리 결과 - beforeSize={}, afterSize={}",
        parsedData.sourceType().name(),
        parsedData.articles().size(),
        parsedNewsArticles.size());
    return parsedNewsArticles;
  }
}

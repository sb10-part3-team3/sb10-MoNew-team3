package com.team3.monew.component.news.filter;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.InterestKeyword;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

  public List<ParsedNewsArticle> filterKeyword(ParsedData parsedData,
      Collection<InterestKeyword> interestKeywords) {

    // 관심사별 묶기
    Map<String, List<InterestKeyword>> interestMap = interestKeywords.stream()
        .collect(Collectors.groupingBy(ik -> ik.getInterest().getName()));

    List<ParsedNewsArticle> parsedNewsArticles = parsedData.articles().stream()
        .filter(article -> {

          Set<String> matchedKeywords = Stream.of(
                  keywordMatch.findMatches(article.title()),
                  keywordMatch.findMatches(article.summary()))
              .flatMap(List::stream)
              .collect(Collectors.toSet());

          boolean isMatched = false;
          // 관심사 비교
          for (String interestName : interestMap.keySet()) {
            if (matchedKeywords.contains(interestName)) {
              // 키워드 비교
              for (InterestKeyword ik : interestMap.get(interestName)) {
                if (matchedKeywords.contains(ik.getKeyword())) {
                  article.interestKeywords().add(ik);
                  isMatched = true;
                }
              }
            }
          }
          return isMatched;
        })
        .toList();

    log.info("{} 기사 filter 처리 결과 - beforeSize={}, afterSize={}",
        parsedData.sourceType().name(),
        parsedData.articles().size(),
        parsedNewsArticles.size());
    return parsedNewsArticles;
  }
}

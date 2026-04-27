package com.team3.monew.component.news.filter;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.entity.InterestKeyword;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
              // 매칭되도 반환값이 기사 원문이라 놓칠 가능성이 있어서 반환값을 전부 소문자 형태로 변경
              .map(keyword -> keyword.toLowerCase(Locale.ROOT))
              .collect(Collectors.toSet());

          boolean isMatched = false;
          // 관심사 비교. 비교 대상의 키워드도 전부 소문자로 변경
          for (String interestName : interestMap.keySet()) {
            if (matchedKeywords.contains(interestName.toLowerCase(Locale.ROOT))) {
              // 키워드 비교
              for (InterestKeyword ik : interestMap.get(interestName)) {
                if (matchedKeywords.contains(ik.getKeyword().toLowerCase(Locale.ROOT))) {
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

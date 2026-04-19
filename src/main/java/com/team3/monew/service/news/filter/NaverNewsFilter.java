package com.team3.monew.service.news.filter;

import com.team3.monew.service.KeywordMatchService;
import com.team3.monew.service.NewsParseService.ParsedData;
import com.team3.monew.service.NewsParseService.ParsedNewsArticle;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NaverNewsFilter implements NewsFilter {

  private final KeywordMatchService keywordMatchService;

  @Override
  public List<ParsedNewsArticle> filterKeyword(List<ParsedData> parsedData, Set<String> keywords) {
    return parsedData.stream()
        .flatMap(data -> data.articles().stream())
        .distinct()   // 여러 query에서 동일한 뉴스기사를 갖고 있을 가능성이 있어 중복 제거 처리
        .filter(article -> {

          Set<String> matchedKeywords = Stream.of(
                  keywordMatchService.findMatches(article.title()),
                  keywordMatchService.findMatches(article.summary()))
              .flatMap(List::stream)
              .collect(Collectors.toSet());

          // set에 데이터가 있어서 article에 저장되면 true
          return article.keywords().addAll(matchedKeywords);
        })
        .toList();
  }
}

package com.team3.monew.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KeywordMatchService {

  private Set<String> currentKeywords;
  private Pattern compiledPattern;

  public void refreshKeywords(Set<String> keywords) {
    // 동일한 키워드면 패스
    if (Objects.equals(currentKeywords, keywords)) {
      return;
    }

    currentKeywords = new HashSet<>(keywords);

    String pattern = keywords.stream()
        .map(Pattern::quote)    // 정규표현식 예약어 escape
        .collect(Collectors.joining("|"));

    // 대소문자 무시
    compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
  }

  public List<String> findMatches(String text) {
    if (compiledPattern == null) {
      return List.of();
    }

    Matcher matcher = compiledPattern.matcher(text);
    return matcher.results()
        .map(MatchResult::group) // 매칭된 텍스트
        .distinct()              // 중복 제거
        .toList();
  }
}

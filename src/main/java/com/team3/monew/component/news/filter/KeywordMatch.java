package com.team3.monew.component.news.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KeywordMatch {

  // 동시 접근 방지용
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private Set<String> currentKeywords;
  private Pattern compiledPattern;

  public void refreshKeywords(Set<String> keywords) {
    lock.writeLock().lock();
    try {
      // 동일한 키워드면 패스
      if (Objects.equals(currentKeywords, keywords)) {
        return;
      }

      // 키워드가 없으면 매칭 불가능하게 설정
      if (keywords.isEmpty()) {
        compiledPattern = null;
        currentKeywords = Set.of();
        return;
      }
      currentKeywords = new HashSet<>(keywords);

      String pattern = keywords.stream()
          .map(Pattern::quote)    // 정규표현식 예약어 escape
          .collect(Collectors.joining("|"));

      // 대소문자 무시
      compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public List<String> findMatches(String text) {
    lock.readLock().lock();
    try {
      if (compiledPattern == null || text == null || text.isBlank()) {
        return List.of();
      }

      Matcher matcher = compiledPattern.matcher(text);
      return matcher.results()
          .map(MatchResult::group) // 매칭된 텍스트
          .distinct()              // 중복 제거
          .toList();
    } finally {
      lock.readLock().unlock();
    }
  }
}

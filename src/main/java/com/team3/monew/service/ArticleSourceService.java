package com.team3.monew.service;

import com.team3.monew.repository.NewsSourceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleSourceService {

  private final NewsSourceRepository newsSourceRepository;

  public List<String> getArticleSources() {
    log.debug("뉴스 기사 출처 목록 조회 요청");

    List<String> articleSources = newsSourceRepository.findAll().stream()
        .map(newsSource -> newsSource.getSourceType().name())
        .distinct()
        .toList();

    log.debug("뉴스 기사 출처 목록 조회 성공 - size={}", articleSources.size());
    return articleSources;
  }
}

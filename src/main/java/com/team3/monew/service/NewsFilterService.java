package com.team3.monew.service;

import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.service.NewsParseService.ParsedData;
import com.team3.monew.service.NewsParseService.ParsedNewsArticle;
import com.team3.monew.service.news.filter.NewsFilter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsFilterService {

  private final Map<String, NewsFilter> newsFilter;

  public List<ParsedNewsArticle> filter(
      NewsSourceType sourceType, List<ParsedData> data, Set<String> keywords) {
    String beanName = sourceType.name().toLowerCase() + "NewsFilter";

    return newsFilter.get(beanName)
        .filterKeyword(data, keywords);
  }
}

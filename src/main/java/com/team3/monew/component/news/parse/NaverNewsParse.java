package com.team3.monew.component.news.parse;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.component.news.record.RawArticleResult;
import com.team3.monew.entity.enums.NewsSourceType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NaverNewsParse implements NewsParse {

  private final ObjectMapper objectMapper = new ObjectMapper()  // json Parsing용
      .registerModule(new JavaTimeModule());

  @Override
  public ParsedData parse(NewsSourceType sourceType, RawArticleResult rawArticle) {
    try {
      NaverApiResponse response = objectMapper.readValue(
          rawArticle.rawData(), NaverApiResponse.class);

      Instant lastBuildDate = Optional.ofNullable(response.lastBuildDate)
          .orElseGet(() -> response.items().stream()
              .map(NaverItem::pubDate)
              .filter(Objects::nonNull)         // null 제외
              .max(Comparator.naturalOrder())   // 최신것
              .orElse(Instant.now()));          // 기사 자체에 pubDate가 없는 경우 현재시간 추출

      List<ParsedNewsArticle> parsedSortedNewsArticles = response.items().stream()
          .map(item -> new ParsedNewsArticle(
              sourceType,
              item.link(),
              item.title(),
              item.pubDate(),
              item.description(),
              new ArrayList<>()))
          // 발행시간 역순 정렬
          .sorted(Comparator.comparing(ParsedNewsArticle::publishedAt).reversed())
          .toList();

      log.info("Naver 파싱 종료");
      return new ParsedData(sourceType, lastBuildDate, rawArticle.page(), parsedSortedNewsArticles);
    } catch (Exception e) {
      log.warn("Naver 파싱 에러: {}", e.getMessage());
      return ParsedData.Empty();
    }
  }

  // Json 파싱용
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NaverApiResponse(
      // 패턴형태: Fri, 17 Apr 2026 01:15:37 +0000(RFC_1123_DATE_TIME)
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss Z", locale = "en_US")
      Instant lastBuildDate,

      List<NaverItem> items) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record NaverItem(
      String title, String link, String description,

      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss Z", locale = "en_US")
      Instant pubDate
  ) {

  }
}

package com.team3.monew.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.service.news.record.RawArticleResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NewsParseService {

  private final ObjectMapper objectMapper = new ObjectMapper()  // json Parsing용
      .registerModule(new JavaTimeModule());
  private final XmlMapper xmlMapper = XmlMapper.builder()       //  xml Parsing용
      .addModule(new JavaTimeModule()).build();

  public ParsedData parse(NewsSourceType sourceType, RawArticleResult rawArticle) {
    return switch (sourceType) {
      case NAVER -> parseNaverArticle(rawArticle.rawData(), rawArticle.page());
      case CHOSUN -> parseChosunArticle(rawArticle.rawData(), rawArticle.page());
    };
  }

  private ParsedData parseNaverArticle(String rawArticle, int page) {
    try {
      NaverApiResponse response = objectMapper.readValue(rawArticle, NaverApiResponse.class);

      Instant lastBuildDate = Optional.ofNullable(response.lastBuildDate)
          .orElseGet(() -> response.items().stream()
              .map(NaverItem::pubDate)
              .filter(Objects::nonNull)
              .max(Comparator.naturalOrder())
              .orElse(Instant.now()));

      List<ParsedNewsArticle> parsedSortedNewsArticles = response.items().stream()
          .map(item -> new ParsedNewsArticle(
              NewsSourceType.NAVER,
              item.link(),
              item.title(),
              item.pubDate(),
              item.description(),
              new ArrayList<>()))
          .sorted(Comparator.comparing(ParsedNewsArticle::publishedAt).reversed())
          .toList();

      return new ParsedData(NewsSourceType.NAVER, lastBuildDate, page, parsedSortedNewsArticles);

    } catch (Exception e) {
      log.warn("Naver 파싱 에러", e);
      return null;
    }
  }


  private ParsedData parseChosunArticle(String rawArticle, int page) {
    try {
      Rss rss = xmlMapper.readValue(rawArticle, Rss.class);

      Instant lastBuildDate = Optional.ofNullable(rss.channel().lastBuildDate)
          .orElseGet(() -> rss.channel().items().stream()
              .map(RssItem::pubDate)
              .filter(Objects::nonNull)         // null 제외
              .max(Comparator.naturalOrder())   // 최신것
              .orElse(Instant.now()));          // 기사 자체에 pubDate가 없는 경우 현재시간 추출

      List<ParsedNewsArticle> parsedSortedNewsArticles = rss.channel().items().stream()
          .map(item -> new ParsedNewsArticle(
              NewsSourceType.CHOSUN,
              item.link(),
              item.title(),
              item.pubDate(),
              item.description(),
              new ArrayList<>()))
          .sorted(Comparator.comparing(ParsedNewsArticle::publishedAt).reversed())
          .toList();

      return new ParsedData(NewsSourceType.CHOSUN, lastBuildDate, page, parsedSortedNewsArticles);

    } catch (Exception e) {
      log.warn("Chosun 파싱 에러", e);
      return null;
    }
  }

  public record ParsedData(NewsSourceType sourceType,
                           Instant lastBuildDate,
                           int page,
                           List<ParsedNewsArticle> articles) {

  }

  public record ParsedNewsArticle(NewsSourceType sourceType,
                                  String link,
                                  String title,
                                  Instant publishedAt,
                                  String summary,
                                  List<String> keywords) {

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


  // RSS 파싱용
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Rss(Channel channel) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Channel(
      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss Z", locale = "en_US")
      Instant lastBuildDate,

      @JacksonXmlProperty(localName = "item")
      @JacksonXmlElementWrapper(useWrapping = false)
      List<RssItem> items
  ) {

  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RssItem(
      String title, String link, String description,

      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss Z", locale = "en_US")
      Instant pubDate) {

  }
}

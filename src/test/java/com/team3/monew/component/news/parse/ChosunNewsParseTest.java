package com.team3.monew.component.news.parse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.RawArticleResult;
import com.team3.monew.entity.enums.NewsSourceType;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ChosunNewsParseTest {

  @InjectMocks
  private ChosunNewsParse newsParse;


  @Test
  @DisplayName("잘못된 데이터가 들어오면 커스텀 빈 객체를 반환한다")
  void shouldReturnCustomEmpty_whenInvalidDataIsProvided() {
    // given
    RawArticleResult invalidData = new RawArticleResult("invalid data", "apple", 0);

    // when
    ParsedData actualData = newsParse.parse(NewsSourceType.CHOSUN, invalidData);

    // then
    assertThat(actualData)
        .extracting(ParsedData::isEmpty)
        .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
        .isEqualTo(true);
  }

  @Test
  @DisplayName("데이터가 들어오면 파싱한 객체를 반환한다")
  void shouldReturnParsedData_whenRawDataIsProvided() {
    // given
    String rawData = """
        <rss>
          <channel>
            <lastBuildDate>Tue, 21 Apr 2026 01:31:42 +0000</lastBuildDate>
            <item>
              <title><![CDATA[ 4월 1~20일 수출 504억 달러로 전년 대비 49.4% 증가...반도체 수출 182.5% 증가 ]]></title>
              <link>https://www.chosun.com/economy/economy_general/2026/04/21/RG3TQVCEMVG45C74VSTDWPGV2A/</link>
              <description><![CDATA[ 지식재산권(IP·Intellectual Property) 블록체인 인프라 스토리(Story)의 이승윤 대표가 도... ]]></description>
              <pubDate>Tue, 21 Apr 2026 01:30:00 +0000</pubDate>
            </item>
          </channel>
        </rss>
        """;
    RawArticleResult parsingData = new RawArticleResult(rawData, null, 0);
    Instant publishedAt = ZonedDateTime
        .parse("Tue, 21 Apr 2026 01:30:00 +0000", DateTimeFormatter.RFC_1123_DATE_TIME)
        .toInstant();

    // when
    ParsedData actualData = newsParse.parse(NewsSourceType.CHOSUN, parsingData);

    // then
    assertThat(actualData)
        .extracting(ParsedData::articles)
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .first()
        .hasFieldOrPropertyWithValue("publishedAt", publishedAt)
        .hasFieldOrPropertyWithValue("link",
            "https://www.chosun.com/economy/economy_general/2026/04/21/RG3TQVCEMVG45C74VSTDWPGV2A/");

  }

  @Test
  @DisplayName("Rawdata에 시간과 관련된 기사가 없으면 현재시간을 사용해 데이터를 반환한다")
  void shouldUseCurrentTIme_whenLastBuildDateAndPublishedAtIsMissing() {
    // given
    String rawData = """
        <rss>
          <channel>
            <item>
              <title><![CDATA[ 4월 1~20일 수출 504억 달러로 전년 대비 49.4% 증가...반도체 수출 182.5% 증가 ]]></title>
              <link>https://www.chosun.com/economy/economy_general/2026/04/21/RG3TQVCEMVG45C74VSTDWPGV2A/</link>
              <description><![CDATA[ 지식재산권(IP·Intellectual Property) 블록체인 인프라 스토리(Story)의 이승윤 대표가 도... ]]></description>
            </item>
          </channel>
        </rss>
        """;
    RawArticleResult parsingData = new RawArticleResult(rawData, null, 0);

    // when
    ParsedData actualData = newsParse.parse(NewsSourceType.CHOSUN, parsingData);

    // then
    assertThat(actualData)
        .extracting(ParsedData::lastBuildDate)
        .asInstanceOf(InstanceOfAssertFactories.INSTANT)
        .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));  // 1초 이내 근접 검사
  }
}

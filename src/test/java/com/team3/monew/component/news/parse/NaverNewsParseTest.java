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
class NaverNewsParseTest {

  @InjectMocks
  private NaverNewsParse newsParse;

  @Test
  @DisplayName("잘못된 데이터가 들어오면 커스텀 빈 객체를 반환한다")
  void shouldReturnCustomEmpty_whenInvalidDataIsProvided() {
    // given
    RawArticleResult invalidData = new RawArticleResult("invalid data", "apple", 0);

    // when
    ParsedData actualData = newsParse.parse(NewsSourceType.NAVER, invalidData);

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
        {
          "lastBuildDate":"Tue, 21 Apr 2026 11:07:03 +0900",
        	"total":12377861,
        	"start":1,
        	"display":100,
        	"items":[
        		{
        			"title":"&quot;1분기 역대급 실적?&quot;…증권株 투자심리 회복 조짐에 기대감↑",
        			"originallink":"https://www.imaeil.com/page/view/2026042110460377972",
        			"link":"https://n.news.naver.com/mnews/article/088/0001006480?sid=101",
        			"description":"금융정보업체 에프앤가이드에 따르면 한국투자증권, 미래에셋증권, NH투자증권, <b>삼성</b>증권, 키움증권 등... 여기에 메모리 반도체 슈퍼사이클 기대 속 <b>삼성</b>전자와 SK하이닉스로 매수세가 집중된 점도 거래대금 증가를... ",
        			"pubDate":"Tue, 21 Apr 2026 11:06:00 +0900"
        		}
          ]
        }
        """;
    RawArticleResult parsingData = new RawArticleResult(rawData, "삼성", 1);
    Instant publishedAt = ZonedDateTime
        .parse("Tue, 21 Apr 2026 11:06:00 +0900", DateTimeFormatter.RFC_1123_DATE_TIME)
        .toInstant();

    // when
    ParsedData actualData = newsParse.parse(NewsSourceType.NAVER, parsingData);

    // then
    assertThat(actualData)
        .extracting(ParsedData::articles)
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .first()
        .hasFieldOrPropertyWithValue("publishedAt", publishedAt)
        .hasFieldOrPropertyWithValue("link",
            "https://n.news.naver.com/mnews/article/088/0001006480?sid=101");

  }

  @Test
  @DisplayName("Rawdata에 시간과 관련된 기사가 없으면 현재시간을 사용해 데이터를 반환한다")
  void shouldUseCurrentTIme_whenLastBuildDateAndPublishedAtIsMissing() {
    // given
    String rawData = """
        {
        	"total":12377861,
        	"start":1,
        	"display":100,
        	"items":[
        		{
        			"title":"&quot;1분기 역대급 실적?&quot;…증권株 투자심리 회복 조짐에 기대감↑",
        			"originallink":"https://www.imaeil.com/page/view/2026042110460377972",
        			"link":"https://n.news.naver.com/mnews/article/088/0001006480?sid=101",
        			"description":"금융정보업체 에프앤가이드에 따르면 한국투자증권, 미래에셋증권, NH투자증권, <b>삼성</b>증권, 키움증권 등... 여기에 메모리 반도체 슈퍼사이클 기대 속 <b>삼성</b>전자와 SK하이닉스로 매수세가 집중된 점도 거래대금 증가를... "
        		}
          ]
        }
        """;
    RawArticleResult parsingData = new RawArticleResult(rawData, null, 0);

    // when
    ParsedData actualData = newsParse.parse(NewsSourceType.CHOSUN, parsingData);

    // then
    assertThat(actualData)
        .extracting(ParsedData::lastBuildDate)
        .asInstanceOf(InstanceOfAssertFactories.INSTANT)
        .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS)); // 1초 이내 근접 검사
  }
}

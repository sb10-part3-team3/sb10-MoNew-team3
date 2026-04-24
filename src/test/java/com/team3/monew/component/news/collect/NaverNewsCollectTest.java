package com.team3.monew.component.news.collect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team3.monew.component.news.parse.NewsParser;
import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import com.team3.monew.config.NaverProperties;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.enums.NewsSourceType;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class NaverNewsCollectTest {

  @Mock
  private NaverProperties naverProperties;
  @Mock
  private NewsParser newsParser;

  @InjectMocks
  private NaverNewsCollect newsCollect;


  private MockWebServer webServer;
  private InterestKeyword samsungKeyword;
  private List<InterestKeyword> samsungKeywordList;

  @BeforeEach
  void setUp() throws IOException {
    webServer = new MockWebServer();
    webServer.start();

    samsungKeyword = InterestKeyword.create(Interest.create("삼성"), "메모리");
    samsungKeywordList = new ArrayList<>(List.of(samsungKeyword));
  }

  @AfterEach
  void tearDrown() throws IOException {
    webServer.shutdown();
  }


  @Test
  @DisplayName("파싱 실패할때 빈 결과를 반환한다")
  void shouldReturnEmpty_whenParsingFails() {
    // given
    String url = webServer.getHostName();
    int port = webServer.getPort();
    ReflectionTestUtils.setField(newsCollect, "naverBaseUrl", url + ":" + port);

    webServer.enqueue(new MockResponse().setBody("Invalid Data"));
    given(newsParser.parse(any(), any())).willReturn(ParsedData.createEmpty());

    WebClient webClient = WebClient.builder()
        .build();

    // when, then
    StepVerifier.create(newsCollect.collect(webClient, NewsSourceType.NAVER, samsungKeywordList))
        .expectNextCount(0) // 데이터 0개
        .verifyComplete();
  }


  @Test
  @DisplayName("첫 수집 키워드면 1페이지 데이터만 반환한다")
  void shouldReturnFirstData_whenFirstKeywordUses() {
    // given
    String url = webServer.getHostName();
    int port = webServer.getPort();
    ReflectionTestUtils.setField(newsCollect, "naverBaseUrl", url + ":" + port);

    List<ParsedNewsArticle> articles = List.of(
        new ParsedNewsArticle(null, null, null, null, null, samsungKeywordList)
    );
    ParsedData parsedData = new ParsedData(null, Instant.now(), 0, articles);

    webServer.enqueue(new MockResponse().setBody("raw data"));
    given(newsParser.parse(any(), any())).willReturn(parsedData);

    WebClient webClient = WebClient.builder()
        .build();

    // when, then
    StepVerifier.create(newsCollect.collect(webClient, NewsSourceType.NAVER, samsungKeywordList))
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  @DisplayName("마지막 수집 시간보다 최신 기사가 있으면 재귀호출한다")
  void shouldRequestMore_whenNewArticlesExist() {
    // given
    String url = webServer.getHostName();
    int port = webServer.getPort();
    ReflectionTestUtils.setField(newsCollect, "naverBaseUrl", url + ":" + port);

    // 키워드별 검색시간 수정
    Map<String, Instant> testAtMap = new HashMap<>();
    testAtMap.put("삼성 메모리", Instant.now().minusSeconds(300));
    ReflectionTestUtils.setField(newsCollect, "lastCollectedAt", testAtMap);

    List<ParsedNewsArticle> articles = List.of(
        new ParsedNewsArticle(null, null, null,
            Instant.now().minusSeconds(100), null, samsungKeywordList)
    );
    ParsedData parsedData1 = new ParsedData(null, Instant.now(), 1, articles);

    webServer.enqueue(new MockResponse().setBody("raw data"));
    webServer.enqueue(new MockResponse().setBody("raw data2"));
    given(newsParser.parse(any(), any()))
        .willReturn(parsedData1)
        .willReturn(ParsedData.createEmpty());

    WebClient webClient = WebClient.builder()
        .build();

    // when, then
    StepVerifier.create(newsCollect.collect(webClient, NewsSourceType.NAVER, samsungKeywordList))
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  @DisplayName("중복된 시간대의 기사를 만나면 재귀를 멈추고 데이터를 반환한다")
  void shouldStopRecursion_whenOverlappingTimeFound() {
    // given
    String url = webServer.getHostName();
    int port = webServer.getPort();
    ReflectionTestUtils.setField(newsCollect, "naverBaseUrl", url + ":" + port);

    // 키워드별 검색시간 수정
    Map<String, Instant> testAtMap = new HashMap<>();
    testAtMap.put("삼성 메모리", Instant.now().minusSeconds(300));
    ReflectionTestUtils.setField(newsCollect, "lastCollectedAt", testAtMap);

    List<ParsedNewsArticle> articles = List.of(
        new ParsedNewsArticle(null, null, null,
            Instant.now().minusSeconds(100), null, samsungKeywordList),
        new ParsedNewsArticle(null, null, null,
            Instant.now().minusSeconds(1000), null, samsungKeywordList)
    );
    ParsedData parsedData1 = new ParsedData(null, Instant.now(), 1, articles);

    webServer.enqueue(new MockResponse().setBody("raw data"));
    given(newsParser.parse(any(), any()))
        .willReturn(parsedData1);

    WebClient webClient = WebClient.builder()
        .build();

    // when, then
    StepVerifier.create(newsCollect.collect(webClient, NewsSourceType.NAVER, samsungKeywordList))
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  @DisplayName("400번대 에러 발생 시 빈 객체를 반환해 프로세스를 유지한다")
  void shouldReturnEmpty_whenClientErrorOccurs() {
    // given
    String url = webServer.getHostName();
    int port = webServer.getPort();
    ReflectionTestUtils.setField(newsCollect, "naverBaseUrl", url + ":" + port);

    webServer.enqueue(new MockResponse().setResponseCode(400).setBody("raw data"));
    WebClient webClient = WebClient.builder()
        .build();

    // when, then
    StepVerifier.create(newsCollect.collect(webClient, NewsSourceType.NAVER, samsungKeywordList))
        .expectNextCount(0)   // data 0개
        .verifyComplete();
  }

  @Test
  @DisplayName("500번대 에러 발생 시 빈 객체를 반환해 프로세스를 유지한다")
  void shouldReturnEmpty_whenAPIServerErrorOccurs() {
    // given
    String url = webServer.getHostName();
    int port = webServer.getPort();
    ReflectionTestUtils.setField(newsCollect, "naverBaseUrl", url + ":" + port);

    webServer.enqueue(new MockResponse().setResponseCode(500).setBody("raw data"));
    webServer.enqueue(new MockResponse().setResponseCode(500).setBody("raw data"));
    webServer.enqueue(new MockResponse().setResponseCode(500).setBody("raw data"));

    WebClient webClient = WebClient.builder()
        .build();

    // when, then
    StepVerifier.create(newsCollect.collect(webClient, NewsSourceType.NAVER, samsungKeywordList))
        .expectNextCount(0)   // data 0개
        .verifyComplete();
  }
}

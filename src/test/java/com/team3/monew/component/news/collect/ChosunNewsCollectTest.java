package com.team3.monew.component.news.collect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.team3.monew.component.news.parse.NewsParser;
import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.ParsedNewsArticle;
import java.io.IOException;
import java.util.List;
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
class ChosunNewsCollectTest {

  @Mock
  private NewsParser newsParser;

  @InjectMocks
  private ChosunNewsCollect newsCollect;

  private MockWebServer webServer;

  @BeforeEach
  void serverUp() throws IOException {
    webServer = new MockWebServer();
    webServer.start();
  }

  @AfterEach
  void serverDown() throws IOException {
    webServer.shutdown();
  }

  @Test
  @DisplayName("키워드가 없어도 api를 호출해서 저장하고 파싱해서 데이터를 반환해야 한다")
  void shouldReturnParsedData_whenKeywordsIsEmpty() {
    // given
    String url = webServer.getHostName();
    int port = webServer.getPort();
    ReflectionTestUtils.setField(newsCollect, "chosunBaseUrl", url + ":" + port);

    List<ParsedNewsArticle> articles = List.of(
        new ParsedNewsArticle(null, null, null, null, null, null)
    );
    ParsedData parsedData = new ParsedData(null, null, 0, articles);
    webServer.enqueue(new MockResponse().setBody("test"));
    given(newsParser.parse(any(), any())).willReturn(parsedData);

    WebClient webClient = WebClient.builder()
        .baseUrl(webServer.url("/").toString())
        .build();

    // when, then
    StepVerifier.create(newsCollect.collect(webClient, null, null))
        .expectNext(parsedData)
        .verifyComplete();
  }

  @Test
  @DisplayName("잘못된 데이터가 들어와 파싱이 실패하면 아무것도 반환하지 않는다")
  void shouldReturnEmpty_whenInvalidRawDataIsProvided() {
    // given
    String url = webServer.getHostName();
    int port = webServer.getPort();
    ReflectionTestUtils.setField(newsCollect, "chosunBaseUrl", url + ":" + port);

    webServer.enqueue(new MockResponse().setBody("Invalid Data"));
    given(newsParser.parse(any(), any())).willReturn(ParsedData.Empty());

    WebClient webClient = WebClient.builder()
        .baseUrl(webServer.url("/").toString())
        .build();

    // when, then
    StepVerifier.create(newsCollect.collect(webClient, null, null))
        .expectNextCount(0) // 데이터 0개
        .verifyComplete();
  }

  @Test
  @DisplayName("400번대 에러 발생 시 빈 객체를 반환해 프로세스를 유지한다")
  void shouldReturnEmpty_whenClientErrorOccurs() {
    // given
    String url = webServer.getHostName();
    int port = webServer.getPort();
    ReflectionTestUtils.setField(newsCollect, "chosunBaseUrl", url + ":" + port);

    webServer.enqueue(new MockResponse().setResponseCode(400));

    WebClient webClient = WebClient.builder()
        .baseUrl(webServer.url("/").toString())
        .build();

    // when, then
    StepVerifier.create(newsCollect.collect(webClient, null, null))
        .expectNextCount(0) // 데이터 0개
        .verifyComplete();
  }

  @Test
  @DisplayName("500번대 에러 발생 시 빈 객체를 반환해 프로세스를 유지한다")
  void shouldReturnEmpty_whenAPIServerErrorOccurs() {
    // given
    String url = webServer.getHostName();
    int port = webServer.getPort();
    ReflectionTestUtils.setField(newsCollect, "chosunBaseUrl", url + ":" + port);

    webServer.enqueue(new MockResponse().setResponseCode(500));
    webServer.enqueue(new MockResponse().setResponseCode(500));
    webServer.enqueue(new MockResponse().setResponseCode(500));

    WebClient webClient = WebClient.builder()
        .baseUrl(webServer.url("/").toString())
        .build();

    // when, then
    StepVerifier.create(newsCollect.collect(webClient, null, null))
        .expectNextCount(0) // 데이터 0개
        .verifyComplete();
  }
}
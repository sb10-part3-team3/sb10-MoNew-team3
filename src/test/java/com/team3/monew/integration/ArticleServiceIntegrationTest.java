package com.team3.monew.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.monew.dto.article.internal.enums.ArticleDirection;
import com.team3.monew.dto.article.internal.enums.ArticleOrderBy;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.service.ArticleService;
import com.team3.monew.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
public class ArticleServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ArticleService articleService;

  private final String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";

  @Test
  @DisplayName("뉴스기사 목록 조회 통합 테스트에 성공합니다")
  void shouldReturnArticlePage_whenAPICalls() throws Exception {
    // when & then
    mockMvc.perform(get("/api/articles")
            .header(REQUEST_USER_ID_HEADER, UUID.randomUUID())
            .param("keyword", "삼성 메모리")
            .param("sourceIn", NewsSourceType.NAVER.name())
            .param("orderBy", ArticleOrderBy.PUBLISH_DATE.toString())
            .param("direction", ArticleDirection.DESC.toString())
            .param("limit", "10"))
        .andExpect(status().isOk());
  }

}

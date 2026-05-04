package com.team3.monew.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.monew.dto.article.internal.enums.ArticleDirection;
import com.team3.monew.dto.article.internal.enums.ArticleOrderBy;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.User;
import com.team3.monew.entity.ArticleInterest;
import com.team3.monew.entity.ArticleView;
import com.team3.monew.entity.Comment;
import com.team3.monew.entity.CommentLike;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.User;
import com.team3.monew.entity.enums.DeleteStatus;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import com.team3.monew.repository.UserRepository;
import com.team3.monew.service.ArticleService;
import com.team3.monew.repository.ArticleInterestRepository;
import com.team3.monew.repository.ArticleViewRepository;
import com.team3.monew.repository.CommentLikeRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.NewsArticleRepository;
import com.team3.monew.repository.NewsSourceRepository;
import com.team3.monew.repository.UserRepository;
import com.team3.monew.support.IntegrationTestSupport;
import java.time.Instant;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Tag("integration")
public class ArticleServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private NewsArticleRepository newsArticleRepository;
  @Autowired
  private NewsSourceRepository newsSourceRepository;
  @Autowired
  private InterestRepository interestRepository;
  @Autowired
  private ArticleViewRepository articleViewRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private CommentRepository commentRepository;
  @Autowired
  private CommentLikeRepository commentLikeRepository;
  @Autowired
  private ArticleInterestRepository articleInterestRepository;
  @Autowired
  private EntityManager em;

  @Autowired
  private MockMvc mockMvc;

  private static final String REQUEST_USER_ID_HEADER = "Monew-Request-User-ID";
  private static final String ARTICLES_BASE_URL = "/api/articles";

  private NewsArticle newsArticle;
  private UUID commentLikeId;


  @BeforeEach
  void setUp() {
    Interest samsungInterest = Interest.create("삼성");
    Interest appleInterest = Interest.create("애플");
    interestRepository.saveAll(List.of(samsungInterest, appleInterest));

    NewsSource naverSource = NewsSource
        .create(NewsSourceType.NAVER.name() + 1, NewsSourceType.NAVER, "baseUrl");
    newsSourceRepository.save(naverSource);

    newsArticle = NewsArticle
        .create(naverSource, "link", "title", Instant.now(), "summary");
    newsArticle.addArticleInterest(samsungInterest, "갤럭시");
    newsArticle.addArticleInterest(samsungInterest, "메모리");
    newsArticle.addArticleInterest(appleInterest, "아이폰");
    newsArticleRepository.save(newsArticle);
    User user1 = User.create("email@naver.com", "닉닉", "@qwer!!");
    User user2 = User.create("user2@gmail.com", "ha", "orange@1234");
    userRepository.saveAll(List.of(user1, user2));

    ArticleView articleView1 = ArticleView.create(newsArticle, user1);
    ArticleView articleView2 = ArticleView.create(newsArticle, user2);
    articleViewRepository.saveAll(List.of(articleView1, articleView2));

    Comment u1comment = Comment.create(newsArticle, user1, "comment1");
    Comment u2comment = Comment.create(newsArticle, user2, "comment2");
    commentRepository.saveAll(List.of(u1comment, u2comment));

    CommentLike u2CommentLike = CommentLike.create(u2comment, user2);
    commentLikeRepository.save(u2CommentLike);
    commentLikeId = u2CommentLike.getId();

    em.flush();
    em.clear();
  }

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
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(0))
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("뉴스기사 단건 조회 시 동일 사용자 재조회에서는 조회수가 증가하지 않는다")
  void shouldNotIncreaseViewCount_whenSameUserViewsTwice() throws Exception {
    // given
    User user = userRepository.saveAndFlush(
        User.create("view-test@example.com", "tester", "test1234!")
    );
    UUID userId = user.getId();
    UUID articleId = newsArticle.getId();

    // when
    mockMvc.perform(get("/api/articles/{articleId}", articleId)
            .header(REQUEST_USER_ID_HEADER, userId))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/articles/{articleId}", articleId)
            .header(REQUEST_USER_ID_HEADER, userId))
        .andExpect(status().isOk());

    // then
    NewsArticle updated = newsArticleRepository.findById(articleId).orElseThrow();
    assertThat(updated.getViewCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("삭제된 기사 조회 시 400 오류가 발생한다")
  void shouldReturn400_whenArticleIsDeleted() throws Exception {
    // given
    User user = userRepository.saveAndFlush(
        User.create("deleted-article-test@example.com", "tester", "test1234!")
    );
    UUID userId = user.getId();
    UUID articleId = newsArticle.getId();

    ReflectionTestUtils.setField(newsArticle, "deleteStatus", DeleteStatus.DELETED);
    newsArticleRepository.saveAndFlush(newsArticle);

    // when & then
    mockMvc.perform(get("/api/articles/{articleId}", articleId)
            .header(REQUEST_USER_ID_HEADER, userId))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("뉴스기사 논리삭제 성공 시 204 반환과 함께 기사의 상태가 softDeleted 상태여야 한다")
  void shouldReturnNoContentAndSoftDeleteArticle_whenDeleteSuccessful() throws Exception {
    // when & then
    mockMvc.perform(delete(ARTICLES_BASE_URL + "/{articleId}", newsArticle.getId()))
        .andExpect(status().isNoContent());

    NewsArticle findNewsArticle = newsArticleRepository.findById(newsArticle.getId())
        .orElseThrow(RuntimeException::new);
    assertThat(findNewsArticle.getDeleteStatus()).isEqualTo(DeleteStatus.DELETED);

    List<ArticleInterest> articleInterests = articleInterestRepository
        .findAllByArticleId(findNewsArticle.getId());
    assertThat(articleInterests).isNotEmpty();

    List<ArticleView> articleViews = articleViewRepository
        .findAllByArticleId(findNewsArticle.getId());
    assertThat(articleViews).isNotEmpty();

    List<Comment> comments = commentRepository.findAllByArticleId(findNewsArticle.getId());
    assertThat(comments).isNotEmpty();
  }

  @Test
  @DisplayName("뉴스기사 물리삭제 성공 시 204 반환과 함께 기사는 DB에서 찾을 수 없어야 한다")
  void shouldReturnNoContentAndDeleteArticle_whenDeletedPhysically() throws Exception {
    // when & then
    mockMvc.perform(delete(ARTICLES_BASE_URL + "/{articleId}/hard", newsArticle.getId()))
        .andExpect(status().isNoContent());

    Optional<NewsArticle> findNewsArticle = newsArticleRepository.findById(newsArticle.getId());
    assertThat(findNewsArticle).isEmpty();

    List<ArticleInterest> articleInterests = articleInterestRepository
        .findAllByArticleId(newsArticle.getId());
    assertThat(articleInterests).isEmpty();

    List<ArticleView> articleViews = articleViewRepository
        .findAllByArticleId(newsArticle.getId());
    assertThat(articleViews).isEmpty();

    List<Comment> comments = commentRepository.findAllByArticleId(newsArticle.getId());
    Optional<CommentLike> commentLike = commentLikeRepository.findById(commentLikeId);
    assertThat(comments).isEmpty();
    assertThat(commentLike).isEmpty();
  }

  @Test
  @DisplayName("논리삭제를 진행할 때 삭제할 기사가 존재하지 않으면 404 NotFound를 반환한다")
  void shouldReturnNotFound_whenSoftDeletingNonExistentArticle() throws Exception {
    // given
    UUID articleId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete(ARTICLES_BASE_URL + "/{articleId}", articleId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ARTICLE_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value("404"))
        .andExpect(jsonPath("$.details.articleId").value(articleId.toString()));
  }

  @Test
  @DisplayName("물리삭제를 진행할 때 삭제할 기사가 존재하지 않으면 404 NotFound를 반환한다")
  void shouldReturnNotFound_whenHardDeletingNonExistentArticle() throws Exception {
    // given
    UUID articleId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete(ARTICLES_BASE_URL + "/{articleId}/hard", articleId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ARTICLE_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value("404"))
        .andExpect(jsonPath("$.details.articleId").value(articleId.toString()));
  }
}

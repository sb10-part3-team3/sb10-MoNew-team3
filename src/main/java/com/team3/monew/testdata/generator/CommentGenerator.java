package com.team3.monew.testdata.generator;

import static org.instancio.Select.field;

import com.team3.monew.entity.Comment;
import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.User;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.instancio.Instancio;
import org.instancio.Model;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("data-gen")
public class CommentGenerator extends AbstractGenerator<Comment> {

  private List<User> userPool = new ArrayList<>(); // 댓글 작성자 후보 사용자 목록
  private List<NewsArticle> articlePool = new ArrayList<>(); // 댓글이 달릴 기사 목록

  public CommentGenerator(
      JdbcTemplate jdbcTemplate,
      @Qualifier("dataGeneratorExecutor") Executor executor
  ) {
    super(jdbcTemplate, executor);
  }

  public void setUsers(List<User> users) {
    if (users == null || users.isEmpty()) {
      throw new IllegalStateException("userPool이 비어 있습니다. setUsers()에 유효한 사용자 목록이 필요합니다.");
    }
    this.userPool = users;
  }

  public void setArticles(List<NewsArticle> articles) {
    if (articles == null || articles.isEmpty()) {
      throw new IllegalStateException("articlePool이 비어 있습니다. setArticles()에 유효한 기사 목록이 필요합니다.");
    }
    this.articlePool = articles;
  }

  @Override
  protected Model<Comment> getModel() {
    return Instancio.of(Comment.class)
        // Comment는 User, NewsArticle을 모두 참조하므로 미리 준비된 pool에서 매핑
        .supply(field(Comment::getArticle), () ->
            articlePool.get(ThreadLocalRandom.current().nextInt(articlePool.size()))
        )
        .supply(field(Comment::getUser), () ->
            userPool.get(ThreadLocalRandom.current().nextInt(userPool.size()))
        )
        .supply(field(Comment::getContent), () -> {
          String content = faker().lorem().sentence(8, 16);
          return content.length() > Comment.MAX_CONTENT_LENGTH
              ? content.substring(0, Comment.MAX_CONTENT_LENGTH)
              : content;
        })
        .supply(field(Comment::getLikeCount), () -> ThreadLocalRandom.current().nextInt(0, 101))
        .toModel();
  }

  @Override
  protected String getSql() {
    return """
        INSERT INTO comments (
          id, article_id, user_id, content, like_count,
          delete_status, deleted_at, created_at, updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
  }

  @Override
  protected void setValues(PreparedStatement ps, Comment comment) throws SQLException {
    Timestamp createdAt = getUniformTimestamp(30);
    long diffMillis = Instant.now().toEpochMilli() - createdAt.getTime();
    long randomOffset = ThreadLocalRandom.current().nextLong(0, diffMillis + 1);
    Timestamp updatedAt = new Timestamp(createdAt.getTime() + randomOffset);

    ps.setObject(1, comment.getId());
    ps.setObject(2, comment.getArticle().getId());
    ps.setObject(3, comment.getUser().getId());
    ps.setString(4, comment.getContent());
    ps.setInt(5, comment.getLikeCount());
    ps.setString(6, "ACTIVE");
    ps.setNull(7, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
    ps.setTimestamp(8, createdAt);
    ps.setTimestamp(9, updatedAt);
  }

  @Override
  public List<Comment> generate(int totalSize, int chunkSize) {
    // 댓글은 선행 데이터인 User, NewsArticle이 모두 준비되어 있어야 생성 가능
    if (userPool.isEmpty()) {
      throw new IllegalStateException("comment 생성 전에 userPool이 먼저 준비되어야 합니다.");
    }
    if (articlePool.isEmpty()) {
      throw new IllegalStateException("comment 생성 전에 articlePool이 먼저 준비되어야 합니다.");
    }

    List<Comment> generatedComments = super.generate(totalSize, chunkSize);
    // 댓글 생성 후 기사별 실제 댓글 수에 맞게 comment_count를 동기화
    syncArticleCommentCounts(generatedComments);
    return generatedComments;
  }

  private void syncArticleCommentCounts(List<Comment> comments) {
    Map<UUID, Integer> countsByArticleId = comments.stream()
        .collect(Collectors.groupingBy(
            comment -> comment.getArticle().getId(),
            Collectors.summingInt(comment -> 1)
        ));

    List<ArticleCommentCountRow> rows = articlePool.stream()
        .map(article -> new ArticleCommentCountRow(
            article.getId(),
            countsByArticleId.getOrDefault(article.getId(), 0)
        ))
        .toList();

    jdbcTemplate.batchUpdate(
        "UPDATE news_articles SET comment_count = ? WHERE id = ?",
        rows,
        rows.size(),
        (ps, row) -> {
          ps.setInt(1, row.commentCount());
          ps.setObject(2, row.articleId());
        }
    );
  }

  private record ArticleCommentCountRow(
      UUID articleId,
      int commentCount
  ) {

  }
}

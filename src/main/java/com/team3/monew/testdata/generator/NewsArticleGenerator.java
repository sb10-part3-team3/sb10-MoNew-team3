package com.team3.monew.testdata.generator;

import static org.instancio.Select.field;

import com.team3.monew.entity.NewsArticle;
import com.team3.monew.entity.NewsSource;
import com.team3.monew.repository.NewsSourceRepository;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.instancio.Instancio;
import org.instancio.Model;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("data-gen")
public class NewsArticleGenerator extends AbstractGenerator<NewsArticle> {

  private final NewsSourceRepository newsSourceRepository;
  private List<NewsSource> sourcePool = new ArrayList<>();

  public NewsArticleGenerator(
      JdbcTemplate jdbcTemplate,
      @Qualifier("dataGeneratorExecutor") Executor executor,
      NewsSourceRepository newsSourceRepository
  ) {
    super(jdbcTemplate, executor);
    this.newsSourceRepository = newsSourceRepository;
  }

  public void setSources(List<NewsSource> sources) {
    if (sources == null || sources.isEmpty()) {
      throw new IllegalStateException("sourcePool이 비어 있습니다. setSources()에 유효한 source 목록이 필요합니다.");
    }
    this.sourcePool = sources;
  }

  @Override
  protected Model<NewsArticle> getModel() {
    return Instancio.of(NewsArticle.class)
        .supply(field(NewsArticle::getSource), () ->
            sourcePool.get(ThreadLocalRandom.current().nextInt(sourcePool.size())))
        .supply(field(NewsArticle::getOriginalLink), () -> {
          NewsSource source = sourcePool.get(ThreadLocalRandom.current().nextInt(sourcePool.size()));
          return source.getBaseUrl() + "/news/" + UUID.randomUUID();
        })
        .supply(field(NewsArticle::getTitle), () -> {
          String title = faker().lorem().sentence(3, 6);
          return title.length() > 500 ? title.substring(0, 500) : title;
        })
        .supply(field(NewsArticle::getSummary), () -> {
          String summary = faker().lorem().paragraph();
          return summary.length() > 1000 ? summary.substring(0, 1000) : summary;
        })
        .supply(field(NewsArticle::getPublishedAt), () ->
            Instant.now().minusSeconds(ThreadLocalRandom.current().nextLong(30L * 24 * 60 * 60)))
        .supply(field(NewsArticle::getCommentCount), () -> 0)
        .supply(field(NewsArticle::getViewCount), () -> ThreadLocalRandom.current().nextInt(0, 5001))
        .toModel();
  }

  @Override
  protected String getSql() {
    return """
        INSERT INTO news_articles (
          id, source_id, original_link, title, published_at, summary,
          comment_count, view_count, delete_status, deleted_at, created_at, updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (original_link) DO NOTHING
        """;
  }

  @Override
  protected void setValues(PreparedStatement ps, NewsArticle article) throws SQLException {
    Timestamp createdAt = getUniformTimestamp(30);
    long diffMillis = Instant.now().toEpochMilli() - createdAt.getTime();
    long randomOffset = ThreadLocalRandom.current().nextLong(0, diffMillis + 1);
    Timestamp updatedAt = new Timestamp(createdAt.getTime() + randomOffset);

    ps.setObject(1, article.getId());
    ps.setObject(2, article.getSource().getId());
    ps.setString(3, article.getOriginalLink());
    ps.setString(4, article.getTitle());
    ps.setTimestamp(5, Timestamp.from(article.getPublishedAt()));
    ps.setString(6, article.getSummary());
    ps.setInt(7, article.getCommentCount());
    ps.setInt(8, article.getViewCount());
    ps.setString(9, "ACTIVE");
    ps.setNull(10, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
    ps.setTimestamp(11, createdAt);
    ps.setTimestamp(12, updatedAt);
  }

  @Override
  public List<NewsArticle> generate(int totalSize, int chunkSize) {
    if (sourcePool.isEmpty()) {
      List<NewsSource> sources = newsSourceRepository.findAll();
      if (sources.isEmpty()) {
        throw new IllegalStateException("news_sources가 비어 있습니다. 뉴스기사 생성 전에 source 데이터가 필요합니다.");
      }
      this.sourcePool = sources;
      log.info("뉴스기사 생성용 source {}건 로드 완료", sources.size());
    }

    return super.generate(totalSize, chunkSize);
  }
}

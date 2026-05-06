package com.team3.monew.testdata.generator;

import static org.instancio.Select.field;

import com.team3.monew.entity.Interest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import org.instancio.Instancio;
import org.instancio.Model;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("data-gen")
public class InterestGenerator extends AbstractGenerator<Interest> {

  private static final List<String> INTEREST_NAMES = List.of(
      "경제", "주식", "부동산", "가상자산", "금융", "정치", "사회", "국제",
      "기술", "인공지능", "IT", "과학", "건강", "스포츠", "문화", "연예",
      "자동차", "게임", "교육", "환경"
  );

  private static final List<String> KEYWORDS = List.of(
      "증시", "환율", "금리", "투자", "부동산", "비트코인", "AI", "반도체",
      "스타트업", "정부", "정책", "선거", "축구", "야구", "영화", "드라마",
      "전기차", "배터리", "기후", "교육"
  );

  public InterestGenerator(
      JdbcTemplate jdbcTemplate,
      @Qualifier("dataGeneratorExecutor") Executor executor
  ) {
    super(jdbcTemplate, executor);
  }

  @Override
  protected Model<Interest> getModel() {
    return Instancio.of(Interest.class)
        .supply(field(Interest::getName), () -> {
          String prefix = KEYWORDS.get(ThreadLocalRandom.current().nextInt(KEYWORDS.size()));
          return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        })
        .supply(field(Interest::getSubscriberCount), () -> 0)
        .toModel();
  }

  @Override
  protected String getSql() {
    return """
        INSERT INTO interests (
          id, name, subscriber_count, created_at, updated_at
        )
        VALUES (?, ?, ?, ?, ?)
        """;
  }

  @Override
  protected void setValues(java.sql.PreparedStatement ps, Interest interest)
      throws java.sql.SQLException {
    Timestamp createdAt = getUniformTimestamp(30);

    long diffMillis = Instant.now().toEpochMilli() - createdAt.getTime();
    long randomOffset = ThreadLocalRandom.current().nextLong(0, diffMillis + 1);
    Timestamp updatedAt = new Timestamp(createdAt.getTime() + randomOffset);

    ps.setObject(1, interest.getId());
    ps.setString(2, interest.getName());
    ps.setInt(3, interest.getSubscriberCount());
    ps.setTimestamp(4, createdAt);
    ps.setTimestamp(5, updatedAt);
  }

  @Override
  public List<Interest> generate(int totalSize, int chunkSize) {
    // 대량 생성 성능을 위해 서비스/JPA cascade를 거치지 않고,
    // interests를 먼저 저장한 뒤 FK(interest_id) 기준으로 interest_keywords를 직접 저장한다.
    // 최종 DB 상태에서는 모든 관심사가 1개 이상의 키워드를 가진다.
    List<Interest> interests = super.generate(totalSize, chunkSize);
    insertKeywords(interests);

    return interests;
  }

  private void insertKeywords(List<Interest> interests) {
    List<InterestKeywordRow> rows = interests.stream()
        .flatMap(interest -> createRandomKeywords(interest).stream())
        .toList();

    jdbcTemplate.batchUpdate(
        """
            INSERT INTO interest_keywords (
              id, interest_id, keyword, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?)
            """,
        rows,
        rows.size(),
        (ps, row) -> {
          ps.setObject(1, row.id());
          ps.setObject(2, row.interestId());
          ps.setString(3, row.keyword());
          ps.setTimestamp(4, row.createdAt());
          ps.setTimestamp(5, row.updatedAt());
        }
    );
  }

  private List<InterestKeywordRow> createRandomKeywords(Interest interest) {
    int keywordCount = ThreadLocalRandom.current().nextInt(1, 4);
    Timestamp now = Timestamp.from(Instant.now());

    return ThreadLocalRandom.current()
        .ints(0, KEYWORDS.size())
        .distinct()
        .limit(keywordCount)
        .mapToObj(KEYWORDS::get)
        .map(keyword -> new InterestKeywordRow(
            UUID.randomUUID(),
            interest.getId(),
            keyword,
            now,
            now
        ))
        .toList();
  }

  private record InterestKeywordRow(
      UUID id,
      UUID interestId,
      String keyword,
      Timestamp createdAt,
      Timestamp updatedAt
  ) {

  }
}
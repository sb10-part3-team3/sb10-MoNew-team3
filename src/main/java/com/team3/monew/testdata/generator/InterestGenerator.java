package com.team3.monew.testdata.generator;

import static org.instancio.Select.field;

import com.team3.monew.entity.Interest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.instancio.Instancio;
import org.instancio.Model;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
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

  private final TransactionTemplate transactionTemplate;

  public InterestGenerator(
      JdbcTemplate jdbcTemplate,
      @Qualifier("dataGeneratorExecutor") Executor executor,
      TransactionTemplate transactionTemplate
  ) {
    super(jdbcTemplate, executor);
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  protected Model<Interest> getModel() {
    return Instancio.of(Interest.class)
        .supply(field(Interest::getName), () -> {
          String prefix = INTEREST_NAMES.get(
              ThreadLocalRandom.current().nextInt(INTEREST_NAMES.size()));
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
    int numTasks = (int) Math.ceil((double) totalSize / chunkSize);

    List<CompletableFuture<List<Interest>>> futures = IntStream.range(0, numTasks)
        .mapToObj(i -> {
          int currentChunkSize = Math.min(chunkSize, totalSize - (i * chunkSize));

          return CompletableFuture.supplyAsync(() ->
              // 비동기 작업 내부에서도 트랜잭션을 보장하기 위해 TransactionTemplate 사용
              // (각 청크 단위로 interests + interest_keywords를 하나의 트랜잭션으로 묶음)
              transactionTemplate.execute(status -> {
                List<Interest> chunk = Instancio.ofList(getModel())
                    .size(currentChunkSize)
                    .create();

                jdbcTemplate.batchUpdate(getSql(), chunk, chunk.size(), this::setValues);
                // 같은 트랜잭션 내에서 키워드 insert
                // → 키워드 삽입 실패 시 관심사도 함께 롤백되어 정합성 보장
                insertKeywords(chunk);

                return chunk;
              }), dataGeneratorExecutor
          ).exceptionally(ex -> {
            log.error("관심사 청크 {}번 생성 중 오류 발생: {}", i, ex.getMessage(), ex);
            return List.of();
          });
        })
        .toList();

    // 모든 비동기 작업 완료 후 결과 flatten
    return futures.stream()
        .map(CompletableFuture::join)
        .filter(chunk -> chunk != null && !chunk.isEmpty())
        .flatMap(List::stream)
        .toList();
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
package com.team3.monew.testdata.generator;

import com.team3.monew.entity.base.BaseEntity;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Model;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 1. 이 클래스를 상속받아 T(엔티티)를 지정합니다.
 * <p>
 * 2. getModel(): Instancio를 사용하여 엔티티 필드 생성 규칙을 정의합니다.
 * <p>
 * 3. getSql(): JDBC용 INSERT 쿼리를 작성합니다. 4. setValues(): PreparedStatement에 엔티티 값을 매핑합니다.
 * <p>
 * 그외
 * <p>
 * - @Profile("data-gen")을 구현 클래스에 붙여주세요.
 * <p>
 * - @Qualifier("dataGeneratorExecutor")를 생성자에 주입받아야 합니다.
 * <p>
 * - 성능을 위해 JDBC batchUpdate를 사용합니다.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractGenerator<T extends BaseEntity> {

  protected final JdbcTemplate jdbcTemplate;
  protected final Executor dataGeneratorExecutor;

  private static final ThreadLocal<Faker> THREAD_LOCAL_FAKER =
      ThreadLocal.withInitial(Faker::new);

  protected Faker faker() {
    return THREAD_LOCAL_FAKER.get();
  }

  // 구현 메서드
  protected abstract Model<T> getModel();

  protected abstract String getSql();

  protected abstract void setValues(PreparedStatement ps, T entity) throws SQLException;

  // 병렬로 데이터 생성 -> 배치로 저장
  public void generate(int totalSize, int chunkSize) {
    int numTasks = (int) Math.ceil((double) totalSize / chunkSize);
    AtomicInteger insertedCount = new AtomicInteger(0); // 삽입 성공 건수 추적

    List<CompletableFuture<Void>> futures = IntStream.range(0, numTasks)
        .mapToObj(i -> {
          int currentChunkSize = Math.min(chunkSize, totalSize - (i * chunkSize));
          return CompletableFuture.runAsync(() -> {
                // 청크 사이즈만큼 생성
                List<T> chunk = Instancio.ofList(getModel()).size(currentChunkSize).create();
                executeBatch(chunk);
                insertedCount.addAndGet(chunk.size());
              }, dataGeneratorExecutor)
              .exceptionally(ex -> {
                log.error("청크 {}번 삽입 중 오류 발생: {}", i, ex.getMessage());
                return null;
              });
        })
        .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    log.info("데이터 생성 작업 완료: 총 {}건 삽입 (요청: {}건)", insertedCount.get(), totalSize);
  }

  private void executeBatch(List<T> entities) {
    jdbcTemplate.batchUpdate(getSql(), entities, entities.size(), this::setValues);
  }
}
package com.team3.monew.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.monew.config.JpaAuditingConfig;
import com.team3.monew.config.QueryDslConfig;
import com.team3.monew.dto.interest.internal.InterestCursor;
import com.team3.monew.dto.interest.internal.InterestSearchCondition;
import com.team3.monew.entity.Interest;
import com.team3.monew.repository.InterestRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({InterestRepositoryImpl.class, JpaAuditingConfig.class, QueryDslConfig.class})
@Tag("integration")
class InterestRepositoryTest {

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private TestEntityManager em;

  @Test
  @DisplayName("검색어가 관심사 이름에 부분 일치하면 조회된다")
  void shouldSearchedByCondition_whenSearchByInterestName() {
    // given
    Interest economy = Interest.create("경제");
    Interest baseball = Interest.create("야구");
    Interest game = Interest.create("게임");

    em.persist(economy);
    em.persist(baseball);
    em.persist(game);
    em.flush();
    em.clear();

    InterestSearchCondition condition = new InterestSearchCondition(
        "게",
        "name",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    // when
    List<Interest> result = interestRepository.searchByCondition(condition);

    // then
    assertThat(result).hasSize(1);
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("게임");
  }

  @Test
  @DisplayName("검색어가 관심사 키워드에 부분 일치하면 조회된다")
  void shouldSearchByCondition_whenSearchByKeyword() {
    // given
    Interest soccer = Interest.create("축구");
    soccer.addKeyword("손흥민");
    soccer.addKeyword("프리미어리그");

    Interest baseball = Interest.create("야구");
    baseball.addKeyword("류현진");

    em.persist(soccer);
    em.persist(baseball);
    em.flush();
    em.clear();

    InterestSearchCondition condition = new InterestSearchCondition(
        "손",
        "name",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    // when
    List<Interest> result = interestRepository.searchByCondition(condition);

    // then
    assertThat(result).hasSize(1);
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("축구");
  }

  @Test
  @DisplayName("관심사 이름 오름차순으로 정렬된다")
  void shouldSearchByCondition_orderByNameAsc() {
    // given
    Interest economy = Interest.create("경제");
    Interest baseball = Interest.create("야구");
    Interest game = Interest.create("게임");

    em.persist(economy);
    em.persist(baseball);
    em.persist(game);
    em.flush();
    em.clear();

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    // when
    List<Interest> result = interestRepository.searchByCondition(condition);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("게임", "경제", "야구");
  }

  @Test
  @DisplayName("관심사 이름 내림차순으로 정렬된다")
  void shouldSearchByCondition_orderByNameDesc() {
    // given
    Interest economy = Interest.create("경제");
    Interest baseball = Interest.create("야구");
    Interest game = Interest.create("게임");

    em.persist(economy);
    em.persist(baseball);
    em.persist(game);
    em.flush();
    em.clear();

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "name",
        "DESC",
        new InterestCursor(null, null),
        10
    );

    // when
    List<Interest> result = interestRepository.searchByCondition(condition);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("야구", "경제", "게임");
  }

  @Test
  @DisplayName("검색어가 없으면 전체 관심사가 조회된다")
  void shouldReturnAllInterests_whenKeywordIsNull() {
    // given
    Interest a = Interest.create("A");
    Interest b = Interest.create("B");
    Interest c = Interest.create("C");

    em.persist(a);
    em.persist(b);
    em.persist(c);
    em.flush();
    em.clear();

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    // when
    List<Interest> result = interestRepository.searchByCondition(condition);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("A", "B", "C");
  }

  @Test
  @DisplayName("구독자 수 오름차순으로 정렬된다")
  void shouldSearchByCondition_orderBySubscriberCountAsc() {
    // given
    Interest a = Interest.create("A");
    Interest b = Interest.create("B");
    Interest c = Interest.create("C");

    ReflectionTestUtils.setField(a, "subscriberCount", 30);
    ReflectionTestUtils.setField(b, "subscriberCount", 10);
    ReflectionTestUtils.setField(c, "subscriberCount", 20);

    em.persist(a);
    em.persist(b);
    em.persist(c);
    em.flush();
    em.clear();

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "subscriberCount",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    // when
    List<Interest> result = interestRepository.searchByCondition(condition);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("B", "C", "A");
  }

  @Test
  @DisplayName("구독자 수 내림차순으로 정렬된다")
  void shouldSearchByCondition_orderBySubscriberCountDesc() {
    // given
    Interest a = Interest.create("A");
    Interest b = Interest.create("B");
    Interest c = Interest.create("C");

    ReflectionTestUtils.setField(a, "subscriberCount", 30);
    ReflectionTestUtils.setField(b, "subscriberCount", 10);
    ReflectionTestUtils.setField(c, "subscriberCount", 20);

    em.persist(a);
    em.persist(b);
    em.persist(c);
    em.flush();
    em.clear();

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "subscriberCount",
        "DESC",
        new InterestCursor(null, null),
        10
    );

    // when
    List<Interest> result = interestRepository.searchByCondition(condition);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("A", "C", "B");
  }

  @Test
  @DisplayName("이름 오름차순 정렬에서 커서 이후 데이터만 조회된다")
  void shouldReturnNextPage_whenCursorIsGivenWithNameAsc() {
    // given
    Interest a = Interest.create("A");
    Interest b = Interest.create("B");
    Interest c = Interest.create("C");
    Interest d = Interest.create("D");

    em.persist(a);
    em.persist(b);
    em.persist(c);
    em.persist(d);
    em.flush();
    em.clear();

    Interest savedB = interestRepository.findAll().stream()
        .filter(interest -> interest.getName().equals("B"))
        .findFirst()
        .orElseThrow();

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(savedB.getName(), savedB.getCreatedAt()),
        10
    );

    // when
    List<Interest> result = interestRepository.searchByCondition(condition);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("C", "D");
  }

  @Test
  @DisplayName("구독자 수 오름차순 정렬에서 커서 이후 데이터만 조회된다")
  void shouldReturnNextPage_whenCursorIsGivenWithSubscriberCountAsc() {
    // given
    Interest a = Interest.create("A");
    Interest b = Interest.create("B");
    Interest c = Interest.create("C");
    Interest d = Interest.create("D");

    ReflectionTestUtils.setField(a, "subscriberCount", 10);
    ReflectionTestUtils.setField(b, "subscriberCount", 20);
    ReflectionTestUtils.setField(c, "subscriberCount", 30);
    ReflectionTestUtils.setField(d, "subscriberCount", 40);

    em.persist(a);
    em.persist(b);
    em.persist(c);
    em.persist(d);
    em.flush();
    em.clear();

    Interest savedB = interestRepository.findAll().stream()
        .filter(interest -> interest.getName().equals("B"))
        .findFirst()
        .orElseThrow();

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "subscriberCount",
        "ASC",
        new InterestCursor(String.valueOf(savedB.getSubscriberCount()), savedB.getCreatedAt()),
        10
    );

    // when
    List<Interest> result = interestRepository.searchByCondition(condition);

    // then
    assertThat(result)
        .extracting(Interest::getName)
        .containsExactly("C", "D");
  }

  @Test
  @DisplayName("검색 조건에 맞는 전체 관심사 개수를 반환한다")
  void shouldCountByCondition() {
    // given
    Interest soccer = Interest.create("축구");
    soccer.addKeyword("손흥민");

    Interest baseball = Interest.create("야구");
    baseball.addKeyword("류현진");

    Interest basketball = Interest.create("농구");

    em.persist(soccer);
    em.persist(baseball);
    em.persist(basketball);
    em.flush();
    em.clear();

    InterestSearchCondition condition = new InterestSearchCondition(
        "구",
        "name",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    // when
    long count = interestRepository.countByCondition(condition);

    // then
    assertThat(count).isEqualTo(3);
  }

  @Test
  @DisplayName("구독 취소 시 구독자 수는 음수가 되지 않는다")
  void shouldNotDecreaseSubscriberCountBelowZero() {
    // given
    Interest interest = interestRepository.save(Interest.create("주식"));

    em.flush();
    em.clear();

    // when
    int updatedCount = interestRepository.decreaseSubscriberCount(interest.getId());

    em.flush();
    em.clear();

    // then
    assertThat(updatedCount).isEqualTo(0);
    Interest foundInterest = interestRepository.findById(interest.getId()).orElseThrow();
    assertThat(foundInterest.getSubscriberCount()).isEqualTo(0);
  }
}
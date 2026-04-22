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
}
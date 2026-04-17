package com.team3.monew.integration;

import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.exception.interest.InterestDuplicateNameException;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.service.InterestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Tag("integration")
public class InterestServiceIntegrationTest {

  @Autowired
  private InterestService interestService;
  @Autowired
  private InterestRepository interestRepository;

  @Test
  @DisplayName("관심사를 등록하면 키워드와 함께 저장된다")
  void shouldRegisterInterest_whenCreateRequest() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "주식",
        List.of("코스피", "삼성전자")
    );

    // when
    InterestDto savedInterest = interestService.create(request);

    // then
    Optional<Interest> found = interestRepository.findById(savedInterest.id());

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("주식");
    assertThat(found.get().getKeywords())
        .extracting(InterestKeyword::getKeyword)
        .containsExactlyInAnyOrder("코스피", "삼성전자");
  }

  @Test
  @DisplayName("이미 같은 이름의 관심사가 존재하면 등록에 실패한다")
  void shouldThrowException_whenDuplicateNameExists() {
    // given
    InterestRegisterRequest request1 = new InterestRegisterRequest(
        "테스트",
        List.of("test", "keyword")
    );

    InterestRegisterRequest request2 = new InterestRegisterRequest(
        "테스트",
        List.of("asdf")
    );

    interestService.create(request1);

    // when & then
    assertThatThrownBy(() -> interestService.create(request2))
        .isInstanceOf(InterestDuplicateNameException.class);
  }

  @Test
  @DisplayName("관심사 이름의 유사도가 80% 이상이면 등록에 실패한다")
  void shouldFailToRegisterInterest_whenNameSimilar80percentOver() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "apple",
        List.of("keyword")
    );

    InterestRegisterRequest request2 = new InterestRegisterRequest(
        "applf",
        List.of("keyword")
    );

    interestService.create(request);

    // when & then
    assertThatThrownBy(() -> interestService.create(request2))
        .isInstanceOf(InterestDuplicateNameException.class);
  }
}

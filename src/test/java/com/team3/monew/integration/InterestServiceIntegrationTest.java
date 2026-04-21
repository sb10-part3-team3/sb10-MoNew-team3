package com.team3.monew.integration;

import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.dto.interest.InterestUpdateRequest;
import com.team3.monew.entity.ArticleInterest;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.Subscription;
import com.team3.monew.exception.interest.InterestDuplicateNameException;
import com.team3.monew.exception.interest.InterestException;
import com.team3.monew.exception.interest.InterestNotFoundException;
import com.team3.monew.repository.ArticleInterestRepository;
import com.team3.monew.repository.InterestKeywordRepository;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.SubscriptionRepository;
import com.team3.monew.service.InterestService;
import java.util.UUID;
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
  @Autowired
  private InterestKeywordRepository interestKeywordRepository;

  @Test
  @DisplayName("관심사를 등록하면 키워드와 함께 저장된다")
  void shouldRegisterInterest_whenCreateRequest() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "주식",
        List.of("코스피", "삼성전자")
    );

    // when
    InterestDto savedInterest = interestService.createInterest(request);

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

    interestService.createInterest(request1);

    // when & then
    assertThatThrownBy(() -> interestService.createInterest(request2))
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

    interestService.createInterest(request);

    // when & then
    assertThatThrownBy(() -> interestService.createInterest(request2))
        .isInstanceOf(InterestDuplicateNameException.class);
  }

  @Test
  @DisplayName("관심사 키워드를 수정하면 기존 키워드는 삭제되고 새 키워드로 교체된다")
  void shouldUpdateKeywords_andReplaceOldKeywords() {
    // given
    InterestRegisterRequest createRequest = new InterestRegisterRequest(
        "주식",
        List.of("코스피", "삼성전자")
    );

    InterestDto saved = interestService.createInterest(createRequest);

    // when
    interestService.updateKeyword(
        UUID.randomUUID(), // userId는 의미 없음 (existsBy는 false로 처리됨)
        saved.id(),
        new InterestUpdateRequest(List.of("나스닥", "애플"))
    );

    // then
    Interest updated = interestRepository.findById(saved.id()).orElseThrow();

    assertThat(updated.getKeywords())
        .extracting(InterestKeyword::getKeyword)
        .containsExactlyInAnyOrder("나스닥", "애플");
  }

  @Test
  @DisplayName("키워드 수정 시 기존 키워드는 orphanRemoval로 삭제된다")
  void shouldDeleteOldKeywords_whenUpdatingKeywords() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "경제",
        List.of("금리", "환율")
    );

    InterestDto saved = interestService.createInterest(request);

    // when
    interestService.updateKeyword(
        UUID.randomUUID(),
        saved.id(),
        new InterestUpdateRequest(List.of("물가"))
    );

    // then
    Interest updated = interestRepository.findById(saved.id()).orElseThrow();

    assertThat(updated.getKeywords())
        .extracting(InterestKeyword::getKeyword)
        .containsExactly("물가");
  }

  @Test
  @DisplayName("키워드가 null이면 수정에 실패한다")
  void shouldFail_whenKeywordsIsNull() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "부동산",
        List.of("아파트")
    );

    InterestDto saved = interestService.createInterest(request);

    // when & then
    assertThatThrownBy(() ->
        interestService.updateKeyword(
            UUID.randomUUID(),
            saved.id(),
            new InterestUpdateRequest(null)
        )
    ).isInstanceOf(InterestException.class);
  }

  @Test
  @DisplayName("키워드가 비어있으면 수정에 실패한다")
  void shouldFail_whenKeywordsIsEmpty() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "코인",
        List.of("비트코인")
    );

    InterestDto saved = interestService.createInterest(request);

    // when & then
    assertThatThrownBy(() ->
        interestService.updateKeyword(
            UUID.randomUUID(),
            saved.id(),
            new InterestUpdateRequest(List.of())
        )
    ).isInstanceOf(InterestException.class);
  }

  @Test
  @DisplayName("공백 키워드가 포함되면 수정에 실패한다")
  void shouldFail_whenKeywordContainsBlank() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "IT",
        List.of("개발")
    );

    InterestDto saved = interestService.createInterest(request);

    // when & then
    assertThatThrownBy(() ->
        interestService.updateKeyword(
            UUID.randomUUID(),
            saved.id(),
            new InterestUpdateRequest(List.of("정상", "   "))
        )
    ).isInstanceOf(InterestException.class);
  }

  @Test
  @DisplayName("중복 키워드가 있으면 수정에 실패한다")
  void shouldFail_whenDuplicateKeywords() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "뉴스",
        List.of("정치")
    );

    InterestDto saved = interestService.createInterest(request);

    // when & then
    assertThatThrownBy(() ->
        interestService.updateKeyword(
            UUID.randomUUID(),
            saved.id(),
            new InterestUpdateRequest(List.of("경제", "경제"))
        )
    ).isInstanceOf(InterestException.class);
  }

  @Test
  @DisplayName("관심사를 삭제할 수 있다")
  void shouldDeleteInterest_whenDeleteRequest() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "삭제용",
        List.of("키워드", "삭제될 키워드")
    );

    InterestDto saved = interestService.createInterest(request);

    // when
    interestService.deleteInterest(saved.id());

    // then
    assertThat(interestRepository.findById(saved.id())).isEmpty();
  }

  @Test
  @DisplayName("존재하지 않는 관심사를 삭제하면 예외가 발생한다")
  void shouldThrowException_whenInterestNotFound() {
    // given
    UUID invalidId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() -> interestService.deleteInterest(invalidId))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("관심사를 삭제하면 연관 키워드도 함께 삭제된다")
  void shouldDeleteInterestWithKeywords_whenDeleteRequest() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "삭제용",
        List.of("키워드", "삭제될 키워드")
    );

    InterestDto saved = interestService.createInterest(request);

    // when
    interestService.deleteInterest(saved.id());

    // then
    assertThat(interestRepository.findById(saved.id())).isEmpty();
    assertThat(interestKeywordRepository.findAllByInterestId(saved.id())).isEmpty();
  }
}

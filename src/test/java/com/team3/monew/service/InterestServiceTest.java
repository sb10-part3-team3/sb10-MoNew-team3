package com.team3.monew.service;

import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.dto.interest.InterestUpdateRequest;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.exception.interest.InterestDuplicateNameException;
import com.team3.monew.exception.interest.InterestException;
import com.team3.monew.exception.interest.InterestNotFoundException;
import com.team3.monew.mapper.InterestMapper;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.SubscriptionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private InterestMapper interestMapper;

  @InjectMocks
  private InterestService interestService;

  @Test
  @DisplayName("관심사를 등록할 수 있다")
  void shouldRegisterInterest_whenCreateRequest() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "주식",
        List.of("코스피", "삼성전자")
    );

    Interest savedInterest = Interest.create("주식");
    savedInterest.addKeyword("코스피");
    savedInterest.addKeyword("삼성전자");

    InterestDto response = new InterestDto(
        UUID.randomUUID(),
        "주식",
        List.of("코스피", "삼성전자"),
        0,
        false
    );

    given(interestRepository.existsByName("주식")).willReturn(false);
    given(interestRepository.findAll()).willReturn(List.of());
    given(interestRepository.saveAndFlush(any(Interest.class))).willReturn(savedInterest);
    given(interestMapper.toDto(savedInterest, false)).willReturn(response);

    // when
    InterestDto result = interestService.create(request);

    // then
    assertThat(result.name()).isEqualTo("주식");
    assertThat(result.keywords()).containsExactlyInAnyOrder("코스피", "삼성전자");
  }

  @Test
  @DisplayName("중복된 관심사 이름은 등록에 실패한다")
  void shouldFailToRegister_whenDuplicateNameExists() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "테스트",
        List.of("test", "keyword")
    );

    given(interestRepository.existsByName("테스트")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> interestService.create(request))
        .isInstanceOf(InterestDuplicateNameException.class);

    then(interestRepository).should(never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("관심사 이름의 유사도가 80% 이상이면 등록에 실패한다")
  void shouldFailToRegisterInterest_whenNameSimilar80percentOver() {
    // given
    InterestRegisterRequest request = new InterestRegisterRequest(
        "applf",
        List.of("keyword")
    );

    Interest existingInterest = Interest.create("apple");

    given(interestRepository.findAll()).willReturn(List.of(existingInterest));
    given(interestRepository.existsByName("applf")).willReturn(false);

    // when & then
    assertThatThrownBy(() -> interestService.create(request))
        .isInstanceOf(InterestDuplicateNameException.class);

    then(interestRepository).should(never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("관심사 키워드를 수정할 수 있다")
  void shouldUpdateInterestKeyword_whenUpdateRequest() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(
        List.of("수정", "키워드수정")
    );

    Interest interest = Interest.create("주식");
    interest.addKeyword("기존키워드");
    interest.addKeyword("기존키워드2");

    InterestDto response = new InterestDto(
        interestId,
        "주식",
        List.of("수정", "키워드수정"),
        0,
        true
    );

    given(interestRepository.findById(interestId)).willReturn((Optional.of(interest)));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)).willReturn(true);
    given(interestMapper.toDto(interest, true)).willReturn(response);

    // when
    InterestDto result = interestService.updateKeyword(userId, interestId, request);

    // then
    assertThat(result.name()).isEqualTo("주식");
    assertThat(result.keywords()).containsExactly("수정", "키워드수정");
    assertThat(result.subscribedByMe()).isTrue();

    assertThat(interest.getKeywords())
        .extracting(InterestKeyword::getKeyword)
        .containsExactly("수정", "키워드수정");

    then(interestRepository).should().findById(interestId);
    then(subscriptionRepository).should().existsByUserIdAndInterestId(userId, interestId);
    then(interestMapper).should().toDto(interest, true);
  }

  @Test
  @DisplayName("존재하지 않는 관심사는 키워드를 수정할 수 없다")
  void shouldFailToUpdateKeyword_whenInterestNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(
        List.of("수정", "키워드수정")
    );

    given(interestRepository.findById(interestId)).willReturn(java.util.Optional.empty());

    // when & then
    assertThatThrownBy(() -> interestService.updateKeyword(userId, interestId, request))
        .isInstanceOf(InterestNotFoundException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(interestMapper).should(never()).toDto(any(), any(Boolean.class));
  }

  @Test
  @DisplayName("키워드 목록이 null이면 관심사 키워드 수정에 실패한다")
  void shouldFailToUpdateKeyword_whenKeywordsIsNull() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(null);

    Interest interest = Interest.create("주식");
    interest.addKeyword("기존키워드");

    given(interestRepository.findById(interestId)).willReturn(java.util.Optional.of(interest));

    // when & then
    assertThatThrownBy(() -> interestService.updateKeyword(userId, interestId, request))
        .isInstanceOf(InterestException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(interestMapper).should(never()).toDto(any(), any(Boolean.class));
  }

  @Test
  @DisplayName("키워드 목록이 비어 있으면 관심사 키워드 수정에 실패한다")
  void shouldFailToUpdateKeyword_whenKeywordsIsEmpty() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(List.of());

    Interest interest = Interest.create("주식");
    interest.addKeyword("기존키워드");

    given(interestRepository.findById(interestId)).willReturn(java.util.Optional.of(interest));

    // when & then
    assertThatThrownBy(() -> interestService.updateKeyword(userId, interestId, request))
        .isInstanceOf(InterestException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(interestMapper).should(never()).toDto(any(), any(Boolean.class));
  }

  @Test
  @DisplayName("공백 문자열이 포함된 키워드가 있으면 관심사 키워드 수정에 실패한다")
  void shouldFailToUpdateKeyword_whenKeywordContainsBlank() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(
        List.of("정상키워드", "   ")
    );

    Interest interest = Interest.create("주식");
    interest.addKeyword("기존키워드");

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));

    // when & then
    assertThatThrownBy(() -> interestService.updateKeyword(userId, interestId, request))
        .isInstanceOf(InterestException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(interestMapper).should(never()).toDto(any(), any(Boolean.class));
  }

  @Test
  @DisplayName("중복된 키워드가 있으면 관심사 키워드 수정에 실패한다")
  void shouldFailToUpdateKeyword_whenKeywordsDuplicated() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(
        List.of("경제", "경제")
    );

    Interest interest = Interest.create("주식");
    interest.addKeyword("기존키워드");

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));

    // when & then
    assertThatThrownBy(() -> interestService.updateKeyword(userId, interestId, request))
        .isInstanceOf(InterestException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(interestMapper).should(never()).toDto(any(), any(Boolean.class));
  }
}
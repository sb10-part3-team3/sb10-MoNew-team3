package com.team3.monew.service;

import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.entity.Interest;
import com.team3.monew.exception.interest.InterestDuplicateNameException;
import com.team3.monew.mapper.InterestMapper;
import com.team3.monew.repository.InterestRepository;
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

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private InterestMapper interestMapper;

  @InjectMocks
  private InterestService interestService;

  @Tag("unit")
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
    given(interestRepository.save(any(Interest.class))).willReturn(savedInterest);
    given(interestMapper.toDto(savedInterest, false)).willReturn(response);

    // when
    InterestDto result = interestService.create(request);

    // then
    assertThat(result.name()).isEqualTo("주식");
    assertThat(result.keywords()).containsExactlyInAnyOrder("코스피", "삼성전자");
  }

  @Tag("unit")
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

    then(interestRepository).should(never()).save(any());
  }

  @Tag("unit")
  @Test
  @DisplayName("관심사 이름의 유사도가 80% 이상이면 등록에 실패한다")
  void shouldFailToRegisterInterest_whenNameSimilar80percenOver() {
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

    then(interestRepository).should(never()).save(any());
  }
}
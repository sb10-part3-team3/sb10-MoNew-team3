package com.team3.monew.service;

import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.dto.interest.InterestUpdateRequest;
import com.team3.monew.dto.interest.SubscriptionDto;
import com.team3.monew.dto.interest.internal.InterestCursor;
import com.team3.monew.dto.interest.internal.InterestSearchCondition;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.InterestKeyword;
import com.team3.monew.entity.Subscription;
import com.team3.monew.entity.User;
import com.team3.monew.exception.interest.InterestDuplicateNameException;
import com.team3.monew.exception.interest.InterestException;
import com.team3.monew.exception.interest.InterestNotFoundException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.mapper.InterestMapper;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.SubscriptionRepository;
import com.team3.monew.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private InterestMapper interestMapper;

  @Mock
  private ApplicationEventPublisher eventPublisher;

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
    InterestDto result = interestService.createInterest(request);

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
    assertThatThrownBy(() -> interestService.createInterest(request))
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
    assertThatThrownBy(() -> interestService.createInterest(request))
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
    given(interestMapper.toDto(interest, null)).willReturn(response);

    // when
    InterestDto result = interestService.updateKeyword(interestId, request);

    // then
    assertThat(result.name()).isEqualTo("주식");
    assertThat(result.keywords()).containsExactly("수정", "키워드수정");
    assertThat(result.subscribedByMe()).isTrue();

    assertThat(interest.getKeywords())
        .extracting(InterestKeyword::getKeyword)
        .containsExactly("수정", "키워드수정");

    then(interestRepository).should().findById(interestId);
    then(interestMapper).should().toDto(interest, null);
  }

  @Test
  @DisplayName("존재하지 않는 관심사는 키워드를 수정할 수 없다")
  void shouldFailToUpdateKeyword_whenInterestNotFound() {
    // given
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(
        List.of("수정", "키워드수정")
    );

    given(interestRepository.findById(interestId)).willReturn(java.util.Optional.empty());

    // when & then
    assertThatThrownBy(() -> interestService.updateKeyword(interestId, request))
        .isInstanceOf(InterestNotFoundException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(interestMapper).should(never()).toDto(any(), any(Boolean.class));
  }

  @Test
  @DisplayName("키워드 목록이 null이면 관심사 키워드 수정에 실패한다")
  void shouldFailToUpdateKeyword_whenKeywordsIsNull() {
    // given
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(null);

    Interest interest = Interest.create("주식");
    interest.addKeyword("기존키워드");

    given(interestRepository.findById(interestId)).willReturn(java.util.Optional.of(interest));

    // when & then
    assertThatThrownBy(() -> interestService.updateKeyword(interestId, request))
        .isInstanceOf(InterestException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(interestMapper).should(never()).toDto(any(), any(Boolean.class));
  }

  @Test
  @DisplayName("키워드 목록이 비어 있으면 관심사 키워드 수정에 실패한다")
  void shouldFailToUpdateKeyword_whenKeywordsIsEmpty() {
    // given
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(List.of());

    Interest interest = Interest.create("주식");
    interest.addKeyword("기존키워드");

    given(interestRepository.findById(interestId)).willReturn(java.util.Optional.of(interest));

    // when & then
    assertThatThrownBy(() -> interestService.updateKeyword(interestId, request))
        .isInstanceOf(InterestException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(interestMapper).should(never()).toDto(any(), any(Boolean.class));
  }

  @Test
  @DisplayName("공백 문자열이 포함된 키워드가 있으면 관심사 키워드 수정에 실패한다")
  void shouldFailToUpdateKeyword_whenKeywordContainsBlank() {
    // given
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(
        List.of("정상키워드", "   ")
    );

    Interest interest = Interest.create("주식");
    interest.addKeyword("기존키워드");

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));

    // when & then
    assertThatThrownBy(() -> interestService.updateKeyword(interestId, request))
        .isInstanceOf(InterestException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(interestMapper).should(never()).toDto(any(), any(Boolean.class));
  }

  @Test
  @DisplayName("중복된 키워드가 있으면 관심사 키워드 수정에 실패한다")
  void shouldFailToUpdateKeyword_whenKeywordsDuplicated() {
    // given
    UUID interestId = UUID.randomUUID();

    InterestUpdateRequest request = new InterestUpdateRequest(
        List.of("경제", "경제")
    );

    Interest interest = Interest.create("주식");
    interest.addKeyword("기존키워드");

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));

    // when & then
    assertThatThrownBy(() -> interestService.updateKeyword(interestId, request))
        .isInstanceOf(InterestException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(interestMapper).should(never()).toDto(any(), any(Boolean.class));
  }

  @Test
  @DisplayName("관심사를 삭제할 수 있다")
  void shouldDeleteInterest_whenDeleteRequest() {
    // given
    UUID interestId = UUID.randomUUID();
    Interest interest = Interest.create("주식");

    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));

    // when
    interestService.deleteInterest(interestId);

    // then
    then(interestRepository).should().delete(interest);
  }

  @Test
  @DisplayName("존재하지 않는 관심사를 삭제하면 예외가 발생한다")
  void shouldThrowException_whenInterestNotFound() {
    // given
    UUID interestId = UUID.randomUUID();
    given(interestRepository.findById(interestId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> interestService.deleteInterest(interestId))
        .isInstanceOf(InterestNotFoundException.class);

    then(interestRepository).should(never()).delete(any());
  }

  @Test
  @DisplayName("limit + 1개 조회 시 hasNext는 true이고 content는 limit만 반환된다")
  void shouldFindAllInterest_whenFindRequest() {
    // given
    UUID userId = UUID.randomUUID();

    Interest i1 = createInterest("가구");
    Interest i2 = createInterest("나무");
    Interest i3 = createInterest("다리");

    List<Interest> result = List.of(i1, i2, i3);

    InterestSearchCondition condition = new InterestSearchCondition(
        null, "name", "ASC", new InterestCursor(null, null), 2
    );

    given(interestRepository.searchByCondition(condition)).willReturn(result);
    given(interestRepository.countByCondition(condition)).willReturn(3L);

    given(interestMapper.toDto(any(), anyBoolean()))
        .willAnswer(invocation ->
            new InterestDto(
                ((Interest) invocation.getArgument(0)).getId(),
                ((Interest) invocation.getArgument(0)).getName(),
                List.of(),
                0,
                false
            ));

    // when
    CursorPageResponseDto<InterestDto> response
        = interestService.findAll(condition, userId);

    // then
    assertThat(response.content()).hasSize(2);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isEqualTo("나무");
    assertThat(response.nextAfter()).isNotNull();
  }

  private Interest createInterest(String name) {
    Interest interest = Interest.create(name);
    ReflectionTestUtils.setField(interest, "createdAt", Instant.now());
    return interest;
  }

  @Test
  @DisplayName("limit 이하 조회 시 hasNext는 false이고 nextCursor는 null이다")
  void shouldFindInterestHasNextIsFalseAndNextCursorIsNull_whenLimit() {
    // given
    UUID userId = UUID.randomUUID();

    Interest i1 = createInterest("가구");
    Interest i2 = createInterest("나무");

    List<Interest> result = List.of(i1, i2);

    InterestSearchCondition condition = new InterestSearchCondition(
        null, "name", "ASC", new InterestCursor(null, null), 3
    );

    given(interestRepository.searchByCondition(condition)).willReturn(result);
    given(interestRepository.countByCondition(condition)).willReturn(2L);

    given(interestMapper.toDto(any(), anyBoolean()))
        .willReturn(new InterestDto(UUID.randomUUID(), "dummy", List.of(), 0, false));

    // when
    CursorPageResponseDto<InterestDto> response =
        interestService.findAll(condition, userId);

    // then
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isNull();
    assertThat(response.nextAfter()).isNull();
  }

  @Test
  @DisplayName("조회 결과가 없으면 빈 리스트를 반환한다")
  void shouldReturnEmptyList_whenResultIsNone() {
    // given
    UUID userId = UUID.randomUUID();

    InterestSearchCondition condition = new InterestSearchCondition(
        null, "name", "ASC", new InterestCursor(null, null), 10
    );

    given(interestRepository.searchByCondition(condition)).willReturn(List.of());
    given(interestRepository.countByCondition(condition)).willReturn(0L);

    // when
    CursorPageResponseDto<InterestDto> response
        = interestService.findAll(condition, userId);

    // then
    assertThat(response.content()).isEmpty();
    assertThat(response.hasNext()).isFalse();

  }

  @Test
  @DisplayName("subscribedByMe가 true로 반영된다")
  void shouldSubscribedByMeIsTrue_whenSubscribing() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    Interest interest = createInterest("economy");
    ReflectionTestUtils.setField(interest, "id", interestId);

    InterestSearchCondition condition = new InterestSearchCondition(
        null, "name", "DESC", new InterestCursor(null, null), 10
    );

    given(interestRepository.searchByCondition(condition)).willReturn(List.of(interest));
    given(interestRepository.countByCondition(condition)).willReturn(1L);

    given(subscriptionRepository.findSubscribedInterestIds(eq(userId), eq(List.of(interestId))))
        .willReturn(Set.of(interestId));

    given(interestMapper.toDto(eq(interest), eq(true)))
        .willReturn(new InterestDto(interestId, "economy", List.of(), 0, true));

    // when
    CursorPageResponseDto<InterestDto> response =
        interestService.findAll(condition, userId);

    // then
    assertThat(response.content().get(0).subscribedByMe()).isTrue();
  }

  @Test
  @DisplayName("관심사를 구독할 수 있다")
  void shouldSubscribeInterest_whenSubscribeRequest() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    User user = User.create("test@example.com", "password", "tester");
    Interest interest = Interest.create("주식");
    ReflectionTestUtils.setField(interest, "id", interestId);

    Subscription savedSubscription = Subscription.create(user, interest);
    ReflectionTestUtils.setField(savedSubscription, "id", UUID.randomUUID());

    SubscriptionDto response = new SubscriptionDto(
        savedSubscription.getId(),
        interestId,
        "주식",
        List.of(),
        1,
        savedSubscription.getCreatedAt()
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(interestRepository.findByIdWithKeywords(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)).willReturn(false);
    given(subscriptionRepository.save(any(Subscription.class))).willReturn(savedSubscription);
    given(interestMapper.toSubscriptionDto(savedSubscription, interest)).willReturn(response);

    // when
    SubscriptionDto result = interestService.subscribe(userId, interestId);

    // then
    assertThat(result.interestId()).isEqualTo(interestId);
    assertThat(result.interestName()).isEqualTo("주식");
    assertThat(result.interestSubscriberCount()).isEqualTo(1);

    then(userRepository).should().findById(userId);
    then(interestRepository).should(times(1)).findById(interestId);
    then(interestRepository).should(times(1)).findByIdWithKeywords(interestId);
    then(subscriptionRepository).should().existsByUserIdAndInterestId(userId, interestId);
    then(subscriptionRepository).should().save(any(Subscription.class));
    then(interestMapper).should().toSubscriptionDto(savedSubscription, interest);
  }

  @Test
  @DisplayName("존재하지 않는 사용자는 관심사를 구독할 수 없다")
  void shouldFailToSubscribe_whenUserNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> interestService.subscribe(userId, interestId))
        .isInstanceOf(UserNotFoundException.class);

    then(interestRepository).should(never()).findById(any());
    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(subscriptionRepository).should(never()).save(any());
    then(interestMapper).should(never()).toSubscriptionDto(any(), any());
  }

  @Test
  @DisplayName("존재하지 않는 관심사는 구독할 수 없다")
  void shouldFailToSubscribe_whenInterestNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    User user = User.create("test@example.com", "password", "tester");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> interestService.subscribe(userId, interestId))
        .isInstanceOf(InterestNotFoundException.class);

    then(subscriptionRepository).should(never()).existsByUserIdAndInterestId(any(), any());
    then(subscriptionRepository).should(never()).save(any());
    then(interestMapper).should(never()).toSubscriptionDto(any(), any());
  }

  @Test
  @DisplayName("이미 구독 중인 관심사는 다시 구독할 수 없다")
  void shouldFailToSubscribe_whenAlreadySubscribing() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    User user = User.create("test@example.com", "password", "tester");
    Interest interest = Interest.create("주식");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)).willReturn(true);

    // when & then
    assertThatThrownBy(() -> interestService.subscribe(userId, interestId))
        .isInstanceOf(InterestException.class)
        .hasMessage(ErrorCode.INTEREST_ALREADY_SUBSCRIBING.getMessage());

    then(subscriptionRepository).should(never()).save(any());
    then(interestMapper).should(never()).toSubscriptionDto(any(), any());
  }

  @Test
  @DisplayName("구독 저장 중 DB unique 제약 위반이 발생하면 중복 구독 예외로 변환된다")
  void shouldFailToSubscribe_whenDataIntegrityViolationOccurs() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    User user = User.create("test@example.com", "password", "tester");
    Interest interest = Interest.create("주식");

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByUserIdAndInterestId(userId, interestId)).willReturn(false);

    org.hibernate.exception.ConstraintViolationException cause =
        new org.hibernate.exception.ConstraintViolationException(
            "unique constraint violation",
            null,
            "uk_subscriptions_user_id_interest_id"
        );

    given(subscriptionRepository.save(any(Subscription.class)))
        .willThrow(new DataIntegrityViolationException("unique constraint violation", cause));

    // when & then
    assertThatThrownBy(() -> interestService.subscribe(userId, interestId))
        .isInstanceOfSatisfying(InterestException.class, e -> {
          assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INTEREST_ALREADY_SUBSCRIBING);
          assertThat(e.getMessage()).isEqualTo(ErrorCode.INTEREST_ALREADY_SUBSCRIBING.getMessage());
        });

    then(userRepository).should().findById(userId);
    then(interestRepository).should().findById(interestId);
    then(subscriptionRepository).should().existsByUserIdAndInterestId(userId, interestId);
    then(subscriptionRepository).should().save(any(Subscription.class));
    then(interestRepository).should(never()).increaseSubscriberCount(any(UUID.class));
    then(interestMapper).should(never()).toSubscriptionDto(any(), any());
  }

  @Test
  @DisplayName("관심사 구독을 취소할 수 있다")
  void shouldCancelSubscribe() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    User user = mock(User.class);
    Interest interest = mock(Interest.class);
    Subscription subscription = mock(Subscription.class);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.findByUserIdAndInterestId(userId, interestId))
        .willReturn(Optional.of(subscription));

    // when
    interestService.cancelSubscribe(userId, interestId);

    // then
    then(subscriptionRepository).should().delete(subscription);
    then(interestRepository).should().decreaseSubscriberCount(interestId);
  }

  @Test
  @DisplayName("존재하지 않는 사용자면 구독 취소에 실패한다")
  void shouldFailToCancelSubscribe_whenUserNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> interestService.cancelSubscribe(userId, interestId))
        .isInstanceOf(UserNotFoundException.class);

    then(interestRepository).should(never()).findById(any());
    then(subscriptionRepository).should(never()).findByUserIdAndInterestId(any(), any());
    then(subscriptionRepository).should(never()).delete(any());
    then(interestRepository).should(never()).decreaseSubscriberCount(any());
  }

  @Test
  @DisplayName("존재하지 않는 관심사면 구독 취소에 실패한다")
  void shouldFailToCancelSubscribe_whenInterestNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    User user = mock(User.class);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> interestService.cancelSubscribe(userId, interestId))
        .isInstanceOf(InterestNotFoundException.class);

    then(subscriptionRepository).should(never()).findByUserIdAndInterestId(any(), any());
    then(subscriptionRepository).should(never()).delete(any());
    then(interestRepository).should(never()).decreaseSubscriberCount(any());
  }

  @Test
  @DisplayName("구독하지 않은 관심사는 구독 취소할 수 없다")
  void shouldFailToCancelSubscribe_whenNotSubscribing() {
    // given
    UUID userId = UUID.randomUUID();
    UUID interestId = UUID.randomUUID();

    User user = mock(User.class);
    Interest interest = mock(Interest.class);

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(interestRepository.findById(interestId)).willReturn(Optional.of(interest));
    given(subscriptionRepository.findByUserIdAndInterestId(userId, interestId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> interestService.cancelSubscribe(userId, interestId))
        .isInstanceOf(InterestException.class);

    then(subscriptionRepository).should(never()).delete(any());
    then(interestRepository).should(never()).decreaseSubscriberCount(any());
  }
}
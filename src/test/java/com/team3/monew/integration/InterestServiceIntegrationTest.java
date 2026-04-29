package com.team3.monew.integration;

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
import com.team3.monew.repository.InterestKeywordRepository;
import com.team3.monew.repository.InterestRepository;
import com.team3.monew.repository.SubscriptionRepository;
import com.team3.monew.repository.UserRepository;
import com.team3.monew.service.InterestService;
import com.team3.monew.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
public class InterestServiceIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private InterestService interestService;
  @Autowired
  private InterestRepository interestRepository;
  @Autowired
  private InterestKeywordRepository interestKeywordRepository;
  @Autowired
  private SubscriptionRepository subscriptionRepository;
  @Autowired
  private UserRepository userRepository;
  @PersistenceContext
  private EntityManager entityManager;

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

  @Nested
  @DisplayName("관심사 이름 유사도 검증 테스트")
  class InterestNameSimilarityTest {

    @BeforeEach
    void setUp() {
      subscriptionRepository.deleteAll();
      interestKeywordRepository.deleteAll();
      interestRepository.deleteAll();

      entityManager.flush();
      entityManager.clear();
    }

    @Test
    @DisplayName("영어 관심사 이름의 유사도가 80% 이상이면 등록에 실패하고 DB에 저장되지 않는다")
    void shouldFailToRegisterInterest_whenEnglishNameSimilar80percentOver() {
      // given
      InterestRegisterRequest appleRequest = new InterestRegisterRequest(
          "apple",
          List.of("keyword")
      );

      InterestRegisterRequest similarRequest = new InterestRegisterRequest(
          "applf",
          List.of("similar")
      );

      interestService.createInterest(appleRequest);

      entityManager.flush();
      entityManager.clear();

      // when & then
      assertThatThrownBy(() -> interestService.createInterest(similarRequest))
          .isInstanceOf(InterestDuplicateNameException.class);

      entityManager.flush();
      entityManager.clear();

      List<Interest> interests = interestRepository.findAll();
      List<InterestKeyword> keywords = interestKeywordRepository.findAll();

      assertThat(interests)
          .extracting(Interest::getName)
          .containsExactly("apple");

      assertThat(keywords)
          .extracting(InterestKeyword::getKeyword)
          .containsExactly("keyword");
    }

    @Test
    @DisplayName("한글 관심사 이름이 4글자 이상이고 자모 분해 기준 유사도가 80% 이상이면 등록에 실패하고 DB에 저장되지 않는다")
    void shouldFailToRegisterInterest_whenKoreanNameSimilar80percentOver() {
      // given
      InterestRegisterRequest samsungRequest = new InterestRegisterRequest(
          "삼성전자",
          List.of("삼성", "전자")
      );

      InterestRegisterRequest similarRequest = new InterestRegisterRequest(
          "삼셩전자",
          List.of("오타", "유사이름")
      );

      interestService.createInterest(samsungRequest);

      entityManager.flush();
      entityManager.clear();

      // when & then
      assertThatThrownBy(() -> interestService.createInterest(similarRequest))
          .isInstanceOf(InterestDuplicateNameException.class);

      entityManager.flush();
      entityManager.clear();

      List<Interest> interests = interestRepository.findAll();
      List<InterestKeyword> keywords = interestKeywordRepository.findAll();

      assertThat(interests)
          .extracting(Interest::getName)
          .containsExactly("삼성전자");

      assertThat(keywords)
          .extracting(InterestKeyword::getKeyword)
          .containsExactlyInAnyOrder("삼성", "전자");
    }

    @Test
    @DisplayName("짧은 한글 관심사 이름은 유사해도 각각 별도 관심사로 저장된다")
    void shouldRegisterBothInterests_whenKoreanNamesAreShortEvenIfSimilar() {
      // given
      InterestRegisterRequest stockRequest = new InterestRegisterRequest(
          "주식",
          List.of("코스피")
      );

      InterestRegisterRequest annotationRequest = new InterestRegisterRequest(
          "주석",
          List.of("문서")
      );

      // when
      InterestDto stock = interestService.createInterest(stockRequest);
      InterestDto annotation = interestService.createInterest(annotationRequest);

      entityManager.flush();
      entityManager.clear();

      // then
      assertThat(interestRepository.findById(stock.id())).isPresent();
      assertThat(interestRepository.findById(annotation.id())).isPresent();

      assertThat(interestRepository.findAll())
          .extracting(Interest::getName)
          .containsExactlyInAnyOrder("주식", "주석");

      assertThat(interestKeywordRepository.findAll())
          .extracting(InterestKeyword::getKeyword)
          .containsExactlyInAnyOrder("코스피", "문서");
    }

    @Test
    @DisplayName("포함 관계인 한글 관심사 이름은 각각 별도 관심사로 저장된다")
    void shouldRegisterBothInterests_whenNameIsContainmentRelationship() {
      // given
      InterestRegisterRequest samsungRequest = new InterestRegisterRequest(
          "삼성",
          List.of("기업")
      );

      InterestRegisterRequest samsungElectronicsRequest = new InterestRegisterRequest(
          "삼성전자",
          List.of("반도체", "전자")
      );

      // when
      InterestDto samsung = interestService.createInterest(samsungRequest);
      InterestDto samsungElectronics = interestService.createInterest(samsungElectronicsRequest);

      entityManager.flush();
      entityManager.clear();

      // then
      assertThat(interestRepository.findById(samsung.id())).isPresent();
      assertThat(interestRepository.findById(samsungElectronics.id())).isPresent();

      assertThat(interestRepository.findAll())
          .extracting(Interest::getName)
          .containsExactlyInAnyOrder("삼성", "삼성전자");

      assertThat(interestKeywordRepository.findAll())
          .extracting(InterestKeyword::getKeyword)
          .containsExactlyInAnyOrder("기업", "반도체", "전자");
    }

    @Test
    @DisplayName("기존 관심사 이름이 요청 관심사 이름을 포함해도 각각 별도 관심사로 저장된다")
    void shouldRegisterBothInterests_whenExistingNameContainsRequestName() {
      // given
      InterestRegisterRequest overseasStockRequest = new InterestRegisterRequest(
          "해외주식",
          List.of("미국주식", "나스닥")
      );

      InterestRegisterRequest stockRequest = new InterestRegisterRequest(
          "주식",
          List.of("코스피")
      );

      // when
      InterestDto overseasStock = interestService.createInterest(overseasStockRequest);
      InterestDto stock = interestService.createInterest(stockRequest);

      entityManager.flush();
      entityManager.clear();

      // then
      assertThat(interestRepository.findById(overseasStock.id())).isPresent();
      assertThat(interestRepository.findById(stock.id())).isPresent();

      assertThat(interestRepository.findAll())
          .extracting(Interest::getName)
          .containsExactlyInAnyOrder("해외주식", "주식");

      assertThat(interestKeywordRepository.findAll())
          .extracting(InterestKeyword::getKeyword)
          .containsExactlyInAnyOrder("미국주식", "나스닥", "코스피");
    }
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

  @Test
  @DisplayName("커서가 없을 때 첫번째 페이지를 보여줘야 한다")
  void shouldReturnFirstPage_whenCursorIsNull() {
    // given
    // 관심사 생성 (정렬 확인 위해 이름 순서 의도적으로 섞음)
    Interest i1 = interestRepository.save(Interest.create("나무"));
    Interest i2 = interestRepository.save(Interest.create("가구"));
    Interest i3 = interestRepository.save(Interest.create("다리"));
    Interest i4 = interestRepository.save(Interest.create("책상"));
    Interest i5 = interestRepository.save(Interest.create("카톡"));

    entityManager.flush();
    entityManager.clear();

    User user = userRepository.save(User.create("test@example.com", "tester", "test1234!"));

    subscriptionRepository.save(Subscription.create(user, i2));

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(null, null),
        2
    );

    // when
    CursorPageResponseDto<InterestDto> response =
        interestService.findAll(condition, user.getId());

    // then
    assertThat(response.content()).hasSize(2);

    // 정렬 확인 (name ASC → 가구, 나무)
    assertThat(response.content())
        .extracting(InterestDto::name)
        .containsExactly("가구", "나무");
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isEqualTo("나무");
    assertThat(response.nextAfter()).isNotNull();
    assertThat(response.totalElements()).isEqualTo(5);

    // subscribedByMe 확인
    Map<String, Boolean> map = response.content().stream()
        .collect(Collectors.toMap(InterestDto::name, InterestDto::subscribedByMe));

    assertThat(map.get("가구")).isTrue();
    assertThat(map.get("나무")).isFalse();
  }

  @Test
  @DisplayName("커서와 after 값으로 다음 페이지를 조회할 수 있다")
  void shouldReturnNextPage_whenCursorAndAfterAreGiven() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );
    UUID userId = user.getId();

    interestRepository.save(Interest.create("A"));
    Interest beta = interestRepository.save(Interest.create("B"));
    interestRepository.save(Interest.create("C"));
    interestRepository.save(Interest.create("D"));
    interestRepository.save(Interest.create("E"));

    entityManager.flush();
    entityManager.clear();

    subscriptionRepository.save(Subscription.create(user, beta));

    InterestSearchCondition firstCondition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(null, null),
        2
    );

    CursorPageResponseDto<InterestDto> firstPage = interestService.findAll(firstCondition, userId);

    InterestSearchCondition secondCondition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(firstPage.nextCursor(), firstPage.nextAfter()),
        2
    );

    // when
    CursorPageResponseDto<InterestDto> secondPage = interestService.findAll(secondCondition,
        userId);

    // then
    assertThat(secondPage.content()).hasSize(2);
    assertThat(secondPage.content())
        .extracting(InterestDto::name)
        .containsExactly("C", "D");
    assertThat(secondPage.hasNext()).isTrue();
    assertThat(secondPage.nextCursor()).isEqualTo("D");
    assertThat(secondPage.nextAfter()).isNotNull();
    assertThat(secondPage.totalElements()).isEqualTo(5);
  }

  @Test
  @DisplayName("더 이상 데이터가 없으면 마지막 페이지를 반환한다")
  void shouldReturnLastPage_whenNoMoreDataExists() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );
    UUID userId = user.getId();

    interestRepository.save(Interest.create("A"));
    interestRepository.save(Interest.create("B"));
    interestRepository.save(Interest.create("C"));
    interestRepository.save(Interest.create("D"));
    interestRepository.save(Interest.create("E"));

    entityManager.flush();
    entityManager.clear();

    InterestSearchCondition firstCondition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(null, null),
        2
    );

    CursorPageResponseDto<InterestDto> firstPage = interestService.findAll(firstCondition, userId);

    InterestSearchCondition secondCondition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(firstPage.nextCursor(), firstPage.nextAfter()),
        2
    );

    CursorPageResponseDto<InterestDto> secondPage = interestService.findAll(secondCondition,
        userId);

    InterestSearchCondition thirdCondition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(secondPage.nextCursor(), secondPage.nextAfter()),
        2
    );

    // when
    CursorPageResponseDto<InterestDto> thirdPage = interestService.findAll(thirdCondition, userId);

    // then
    assertThat(thirdPage.content()).hasSize(1);
    assertThat(thirdPage.content())
        .extracting(InterestDto::name)
        .containsExactly("E");
    assertThat(thirdPage.hasNext()).isFalse();
    assertThat(thirdPage.nextCursor()).isNull();
    assertThat(thirdPage.nextAfter()).isNull();
    assertThat(thirdPage.totalElements()).isEqualTo(5);
  }

  @Test
  @DisplayName("검색어가 관심사 이름에 일치하면 필터링된 결과를 반환한다")
  void shouldReturnFilteredPage_whenSearchKeywordMatchesInterestName() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );
    UUID userId = user.getId();

    interestRepository.save(Interest.create("축구"));
    interestRepository.save(Interest.create("야구"));
    interestRepository.save(Interest.create("농구"));

    InterestSearchCondition condition = new InterestSearchCondition(
        "구",
        "name",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    // when
    CursorPageResponseDto<InterestDto> response = interestService.findAll(condition, userId);

    // then
    assertThat(response.content())
        .extracting(InterestDto::name)
        .containsExactly("농구", "야구", "축구");
    assertThat(response.totalElements()).isEqualTo(3);
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isNull();
    assertThat(response.nextAfter()).isNull();
  }

  @Test
  @DisplayName("검색어가 관심사 키워드에 일치하면 필터링된 결과를 반환한다")
  void shouldReturnFilteredPage_whenSearchKeywordMatchesInterestKeyword() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );
    UUID userId = user.getId();

    Interest soccer = Interest.create("축구");
    soccer.addKeyword("손흥민");
    soccer.addKeyword("프리미어리그");
    interestRepository.save(soccer);

    Interest baseball = Interest.create("야구");
    baseball.addKeyword("류현진");
    interestRepository.save(baseball);

    InterestSearchCondition condition = new InterestSearchCondition(
        "손",
        "name",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    // when
    CursorPageResponseDto<InterestDto> response = interestService.findAll(condition, userId);

    // then
    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).name()).isEqualTo("축구");
    assertThat(response.totalElements()).isEqualTo(1);
    assertThat(response.hasNext()).isFalse();
  }

  @Test
  @DisplayName("사용자가 구독한 관심사는 subscribedByMe가 true로 설정된다")
  void shouldSetSubscribedByMeTrue_whenUserSubscribedInterestExists() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );
    UUID userId = user.getId();

    Interest furniture = interestRepository.save(Interest.create("가구"));
    Interest tree = interestRepository.save(Interest.create("나무"));

    subscriptionRepository.save(Subscription.create(user, furniture));

    InterestSearchCondition condition = new InterestSearchCondition(
        null,
        "name",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    // when
    CursorPageResponseDto<InterestDto> response = interestService.findAll(condition, userId);

    // then
    assertThat(response.content()).hasSize(2);

    Map<String, Boolean> subscribedMap = response.content().stream()
        .collect(Collectors.toMap(InterestDto::name, InterestDto::subscribedByMe));

    assertThat(subscribedMap.get("가구")).isTrue();
    assertThat(subscribedMap.get("나무")).isFalse();
  }

  @Test
  @DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
  void shouldReturnEmptyPage_whenNoInterestMatchesKeyword() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );
    UUID userId = user.getId();

    interestRepository.save(Interest.create("축구"));
    interestRepository.save(Interest.create("야구"));

    InterestSearchCondition condition = new InterestSearchCondition(
        "경제",
        "name",
        "ASC",
        new InterestCursor(null, null),
        10
    );

    // when
    CursorPageResponseDto<InterestDto> response = interestService.findAll(condition, userId);

    // then
    assertThat(response.content()).isEmpty();
    assertThat(response.totalElements()).isEqualTo(0);
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isNull();
    assertThat(response.nextAfter()).isNull();
  }

  @Test
  @DisplayName("관심사를 구독할 수 있다")
  void shouldSubscribeInterest_whenSubscribeRequest() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );

    Interest interest = interestRepository.save(Interest.create("주식"));

    // when
    SubscriptionDto response = interestService.subscribe(user.getId(), interest.getId());

    // then
    Interest foundInterest = interestRepository.findById(interest.getId()).orElseThrow();
    List<Subscription> subscriptions = subscriptionRepository.findAll();

    assertThat(response.interestId()).isEqualTo(interest.getId());
    assertThat(response.interestName()).isEqualTo("주식");
    assertThat(response.interestSubscriberCount()).isEqualTo(1);
    assertThat(response.createdAt()).isNotNull();

    assertThat(foundInterest.getSubscriberCount()).isEqualTo(1);
    assertThat(subscriptions).hasSize(1);
    assertThat(subscriptions.get(0).getUser().getId()).isEqualTo(user.getId());
    assertThat(subscriptions.get(0).getInterest().getId()).isEqualTo(interest.getId());
  }

  @Test
  @DisplayName("존재하지 않는 사용자는 관심사를 구독할 수 없다")
  void shouldFailToSubscribe_whenUserNotFound() {
    // given
    UUID invalidUserId = UUID.randomUUID();
    Interest interest = interestRepository.save(Interest.create("주식"));

    // when & then
    assertThatThrownBy(() -> interestService.subscribe(invalidUserId, interest.getId()))
        .isInstanceOf(UserNotFoundException.class);

    Interest foundInterest = interestRepository.findById(interest.getId()).orElseThrow();
    assertThat(foundInterest.getSubscriberCount()).isEqualTo(0);
    assertThat(subscriptionRepository.findAll()).isEmpty();
  }

  @Test
  @DisplayName("존재하지 않는 관심사는 구독할 수 없다")
  void shouldFailToSubscribe_whenInterestNotFound() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );
    UUID invalidInterestId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() -> interestService.subscribe(user.getId(), invalidInterestId))
        .isInstanceOf(InterestNotFoundException.class);

    assertThat(subscriptionRepository.findAll()).isEmpty();
  }

  @Test
  @DisplayName("이미 구독 중인 관심사는 다시 구독할 수 없다")
  void shouldFailToSubscribe_whenAlreadySubscribing() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );
    Interest interest = interestRepository.save(Interest.create("주식"));

    subscriptionRepository.save(Subscription.create(user, interest));
    interestRepository.increaseSubscriberCount(interest.getId());

    entityManager.flush();
    entityManager.clear();

    // when & then
    assertThatThrownBy(() -> interestService.subscribe(user.getId(), interest.getId()))
        .isInstanceOf(InterestException.class);

    Interest foundInterest = interestRepository.findById(interest.getId()).orElseThrow();
    assertThat(foundInterest.getSubscriberCount()).isEqualTo(1);
    assertThat(subscriptionRepository.findAll()).hasSize(1);
  }

  @Test
  @DisplayName("관심사 구독을 취소하면 구독 정보가 삭제되고 구독자 수가 감소한다")
  void shouldCancelSubscriber() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );

    Interest interest = interestRepository.save(Interest.create("주식"));

    Subscription subscription = subscriptionRepository.save(
        Subscription.create(user, interest)
    );

    interestRepository.increaseSubscriberCount(interest.getId());

    entityManager.flush();
    entityManager.clear();

    // when
    interestService.cancelSubscribe(user.getId(), interest.getId());

    entityManager.flush();
    entityManager.clear();

    // then
    assertThat(subscriptionRepository.findById(subscription.getId())).isEmpty();
    assertThat(subscriptionRepository.findByUserIdAndInterestId(
        user.getId(), interest.getId()
    )).isEmpty();

    Interest foundInterest = interestRepository.findById(interest.getId())
        .orElseThrow();
    assertThat(foundInterest.getSubscriberCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("구독하지 않은 관심사는 구독 취소할 수 없다")
  void shouldFailToCancelSubscribe_whenNotSubscribing() {
    // given
    User user = userRepository.save(
        User.create("test@example.com", "tester", "test1234!")
    );

    Interest interest = interestRepository.save(
        Interest.create("주식")
    );

    entityManager.flush();
    entityManager.clear();

    // when & then
    assertThatThrownBy(() -> interestService.cancelSubscribe(user.getId(), interest.getId()))
        .isInstanceOf(InterestException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INTEREST_NOT_SUBSCRIBING);

    assertThat(subscriptionRepository.findByUserIdAndInterestId(user.getId(), interest.getId()))
        .isEmpty();

    Interest foundInterest = interestRepository.findById(interest.getId())
        .orElseThrow();
    assertThat(foundInterest.getSubscriberCount()).isEqualTo(0);
  }
}

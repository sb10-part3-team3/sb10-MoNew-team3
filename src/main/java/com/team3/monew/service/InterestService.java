package com.team3.monew.service;

import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.dto.interest.InterestUpdateRequest;
import com.team3.monew.dto.interest.SubscriptionDto;
import com.team3.monew.dto.interest.internal.InterestSearchCondition;
import com.team3.monew.dto.pagination.CursorPageResponseDto;
import com.team3.monew.entity.Interest;
import com.team3.monew.entity.Subscription;
import com.team3.monew.entity.User;
import com.team3.monew.event.SubscriptionEvent;
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
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class InterestService {

  private final InterestRepository interestRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final InterestMapper interestMapper;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  public InterestDto createInterest(InterestRegisterRequest dto) {
    log.debug("관심사 등록 요청 - name={}, keywordCount={}",
        dto.name(), dto.keywords().size());

    if (interestRepository.existsByName(dto.name())) {
      log.warn("관심사 등록 실패 - 중복된 이름, name={}", dto.name());
      throw new InterestDuplicateNameException();
    }

    List<Interest> interests = interestRepository.findAll();
    for (Interest interest : interests) {
      double similarity = calculateSimilarity(interest.getName(), dto.name());
      if (similarity >= 0.8) {
        log.warn("관심사 등록 실패 - 유사한 이름 존재, requestName={}, existingName={}, similarity={}",
            dto.name(), interest.getName(), similarity);
        throw new InterestDuplicateNameException();
      }
    }

    Interest interest = Interest.create(dto.name());
    dto.keywords().forEach(interest::addKeyword);

    Interest savedInterest;
    try {
      savedInterest = interestRepository.saveAndFlush(interest);
    } catch (DataIntegrityViolationException e) {
      log.warn("관심사 등록 실패 - DB unique 제약 위반, name={}", dto.name());
      throw new InterestDuplicateNameException();
    }

    log.info("관심사 등록 성공 - interestId={}, name={}",
        savedInterest.getId(), savedInterest.getName());

    return interestMapper.toDto(savedInterest, false);
  }

  @Transactional(readOnly = true)
  public CursorPageResponseDto<InterestDto> findAll(InterestSearchCondition condition,
      UUID userId) {
    log.debug(
        "관심사 목록 조회 요청 - userId={}, keyword={}, orderBy={}, direction={}, cursor={}, after={}, limit={}",
        userId,
        condition.keyword(),
        condition.orderBy(),
        condition.direction(),
        condition.cursor() == null ? null : condition.cursor().cursor(),
        condition.cursor() == null ? null : condition.cursor().after(),
        condition.limit());

    List<Interest> result = interestRepository.searchByCondition(condition);
    boolean hasNext = result.size() > condition.limit();
    List<Interest> content = hasNext
        ? result.subList(0, condition.limit())
        : result;

    long totalElements = interestRepository.countByCondition(condition);

    if (content.isEmpty()) {
      log.debug("관심사 목록 조회 결과 없음 - userId={}, keyword={}, totalElements=0",
          userId, condition.keyword());

      return new CursorPageResponseDto<InterestDto>(
          List.of(),
          null,
          null,
          condition.limit(),
          totalElements,
          false
      );
    }

    String nextCursor = null;
    Instant nextAfter = null;

    if (hasNext) {
      Interest last = content.get(content.size() - 1);

      if ("subscriberCount".equals(condition.orderBy())) {
        nextCursor = String.valueOf(last.getSubscriberCount());
      } else {
        nextCursor = last.getName();
      }

      nextAfter = last.getCreatedAt();
    }

    List<UUID> interestIds = content.stream()
        .map(Interest::getId)
        .toList();

    Set<UUID> subscribedInterestIds =
        subscriptionRepository.findSubscribedInterestIds(userId, interestIds);

    List<InterestDto> dtoList = content.stream()
        .map(interest -> {
          boolean subscribedByMe = subscribedInterestIds.contains(interest.getId());
          return interestMapper.toDto(interest, subscribedByMe);
        })
        .toList();

    log.debug(
        "관심사 목록 조회 성공 - userId={}, contentSize={}, totalElements={}, hasNext={}, nextCursor={}, nextAfter={}",
        userId, dtoList.size(), totalElements, hasNext, nextCursor, nextAfter);

    return new CursorPageResponseDto<InterestDto>(
        dtoList,
        nextCursor,
        nextAfter,
        condition.limit(),
        totalElements,
        hasNext
    );
  }

  public InterestDto updateKeyword(UUID interestId, InterestUpdateRequest dto) {
    log.debug("관심사 키워드 수정 요청 - interestId={}, keywordCount={}",
        interestId, dto.keywords() == null ? 0 : dto.keywords().size());

    Interest interest = findInterestOrElseThrow(interestId);

    // 빈 리스트
    if (dto.keywords() == null || dto.keywords().isEmpty()) {
      log.warn("관심사 키워드 수정 실패 - 키워드 리스트 비어있음, interestId={}", interestId);
      throw new InterestException(ErrorCode.INTEREST_KEYWORD_LIST_IS_BLANK);
    }

    // NPE 방지
    if (dto.keywords().stream().anyMatch(Objects::isNull)) {
      log.warn("관심사 키워드 수정 실패 - null 키워드 포함, interestId={}", interestId);
      throw new InterestException(ErrorCode.INTEREST_KEYWORD_LIST_IS_BLANK);
    }

    List<String> keywords = dto.keywords().stream()
        .map(String::trim)
        .toList();

    // 공백 키워드
    if (keywords.stream().anyMatch(String::isBlank)) {
      log.warn("관심사 키워드 수정 실패 - 공백 키워드 포함, interestId={}, keywordsCount={}",
          interestId, keywords.size());
      throw new InterestException(ErrorCode.INTEREST_KEYWORD_LIST_IS_BLANK);
    }

    // 중복 키워드
    if (keywords.stream().distinct().count() != keywords.size()) {
      log.warn("관심사 키워드 수정 실패 - 중복 키워드 존재, interestId={}, keywordsCount={}",
          interestId, keywords.size());
      throw new InterestException(ErrorCode.INTEREST_KEYWORD_DUPLICATED);
    }

    // 기존 키워드와 dto의 키워드중 중복되는게 있을 경우 지워지지 않는 경우를 대비
    interest.getKeywords().clear();
    interestRepository.flush();

    interest.updateKeywords(keywords);

    log.info("관심사 키워드 수정 성공 - interestId={}, updatedKeywordsCount={}, subscribedByMe={}",
        interestId, keywords.size(), null);

    return interestMapper.toDto(interest, null);
  }

  public void deleteInterest(UUID interestId) {
    log.debug("관심사 삭제 요청 - interestId={}", interestId);
    Interest interest = findInterestOrElseThrow(interestId);

    interestRepository.delete(interest);

    log.info("관심사 삭제 성공 - interestId={}", interestId);
  }

  public SubscriptionDto subscribe(UUID userId, UUID interestId) {
    log.debug("관심사 구독 요청 - interestId={}", interestId);
    User user = findUserOrElseThrow(userId);
    Interest interest = findInterestOrElseThrow(interestId);

    boolean isSubscribed = subscriptionRepository.existsByUserIdAndInterestId(userId, interestId);

    if (isSubscribed) {
      log.warn("관심사 구독 실패 - 이미 구독중인 관심사, interestId={}", interestId);
      throw new InterestException(ErrorCode.INTEREST_ALREADY_SUBSCRIBING);
    }

    try {
      Subscription subscription = Subscription.create(user, interest);
      Subscription savedSubscription = subscriptionRepository.save(subscription);

      interestRepository.increaseSubscriberCount(interestId);
      Interest updatedInterest = findInterestWithKeywordsOrElseThrow(interestId);

      log.debug("관심사 구독 성공 - userId={}, interestId={}", userId, interestId);
      // 구독 이벤트 발행
      eventPublisher.publishEvent(SubscriptionEvent.from(savedSubscription, updatedInterest));
      return interestMapper.toSubscriptionDto(savedSubscription, updatedInterest);
    } catch (DataIntegrityViolationException e) {
      if (isDuplicateSubscriptionViolation(e)) {
        log.warn("관심사 구독 실패 - 중복 구독 충돌, userId={}, interestId={}", userId, interestId);
        throw new InterestException(ErrorCode.INTEREST_ALREADY_SUBSCRIBING);
      }
      throw e;
    }
  }

  public void cancelSubscribe(UUID userId, UUID interestId) {
    log.debug("관심사 구독 취소 요청 - interestId={}", interestId);
    findUserOrElseThrow(userId);
    findInterestOrElseThrow(interestId);
    Subscription subscription
        = subscriptionRepository.findByUserIdAndInterestId(userId, interestId)
        .orElseThrow(() -> new InterestException(ErrorCode.INTEREST_NOT_SUBSCRIBING));

    subscriptionRepository.delete(subscription);
    interestRepository.decreaseSubscriberCount(interestId);

    log.info("관심사 구독 취소 성공 - interestId={}", interestId);
  }

  private Interest findInterestOrElseThrow(UUID interestId) {
    return interestRepository.findById(interestId)
        .orElseThrow(InterestNotFoundException::new);
  }

  private Interest findInterestWithKeywordsOrElseThrow(UUID interestId) {
    return interestRepository.findByIdWithKeywords(interestId)
        .orElseThrow(InterestNotFoundException::new);
  }

  private User findUserOrElseThrow(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
  }

  // 유니크 제약 위반 검증
  private boolean isDuplicateSubscriptionViolation(DataIntegrityViolationException e) {
    Throwable cause = e;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException cve) {
        return "uk_subscriptions_user_id_interest_id".equals(cve.getConstraintName());
      }
      cause = cause.getCause();
    }
    return false;
  }

  // 유사도 계산 메서드
  private double calculateSimilarity(String a, String b) {
    String normalizedA = normalize(a);
    String normalizedB = normalize(b);
    int distance = levenshtein(normalizedA, normalizedB);
    int maxLength = Math.max(normalizedA.length(), normalizedB.length());

    if (maxLength == 0) {
      // 둘 다 빈 문자열이면 동일한 것으로 간주함
      return 1.0;
    }

    return 1.0 - ((double) distance / maxLength);
  }

  // 문자열 정규화
  private String normalize(String s) {
    return s.trim().toLowerCase();
  }

  // 레벤슈타인 알고리즘
  private int levenshtein(String a, String b) {
    int[][] dp = new int[a.length() + 1][b.length() + 1];

    for (int i = 0; i <= a.length(); i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= b.length(); j++) {
      dp[0][j] = j;
    }

    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;

        dp[i][j] = Math.min(
            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
            dp[i - 1][j - 1] + cost
        );
      }
    }

    return dp[a.length()][b.length()];
  }
}

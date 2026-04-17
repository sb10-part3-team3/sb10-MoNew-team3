package com.team3.monew.service;

import com.team3.monew.dto.interest.InterestDto;
import com.team3.monew.dto.interest.InterestRegisterRequest;
import com.team3.monew.entity.Interest;
import com.team3.monew.exception.interest.InterestDuplicateNameException;
import com.team3.monew.exception.interest.InterestNotFoundException;
import com.team3.monew.mapper.InterestMapper;
import com.team3.monew.repository.InterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final InterestMapper interestMapper;

  public InterestDto create(InterestRegisterRequest dto) {
    log.info("관심사 등록 요청 - name={}, keywordCount={}",
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

  private Interest findInterestOrElseThrow(UUID interestId) {
    return interestRepository.findById(interestId)
        .orElseThrow(InterestNotFoundException::new);
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

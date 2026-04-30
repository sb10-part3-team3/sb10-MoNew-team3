package com.team3.monew.service;

import com.team3.monew.dto.user.UserLoginRequest;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.dto.user.UserDto;
import com.team3.monew.dto.user.UserUpdateRequest;
import com.team3.monew.entity.User;
import com.team3.monew.event.UserDeletedEvent;
import com.team3.monew.event.UserRegisteredEvent;
import com.team3.monew.event.UserUpdatedEvent;
import com.team3.monew.exception.user.AuthException;
import com.team3.monew.exception.user.DeletedUserException;
import com.team3.monew.exception.user.DuplicateEmailException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.mapper.UserMapper;
import com.team3.monew.repository.CommentLikeRepository;
import com.team3.monew.repository.CommentRepository;
import com.team3.monew.repository.NotificationRepository;
import com.team3.monew.repository.SubscriptionRepository;
import com.team3.monew.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final CommentRepository commentRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Transactional
  public UserDto registerUser(UserRegisterRequest userRegisterRequest) {
    log.debug("사용자 등록 요청: email={}", userRegisterRequest.email());

    validateDuplicateEmail(userRegisterRequest.email());

    User user = userMapper.toEntity(userRegisterRequest);
    String encodedPassword = passwordEncoder.encode(userRegisterRequest.password());
    user.changePassword(encodedPassword);

    // 동시성 문제로 DB레벨 중복 예외 발생 시 예외 전환
    try {
      User savedUser = userRepository.save(user);
      log.debug("사용자 등록 성공: userId={}, email={}", savedUser.getId(), savedUser.getEmail());

      // 사용자 등록 이벤트 발행
      applicationEventPublisher.publishEvent(UserRegisteredEvent.from(savedUser));
      return userMapper.toDto(savedUser);
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateEmailException();
    }
  }

  public UserDto loginUser(UserLoginRequest userLoginRequest) {
    log.debug("사용자 로그인 요청: email={},", userLoginRequest.email());
    User user = userRepository.findByEmail(userLoginRequest.email())
        .orElseThrow(AuthException::new);
    if (user.isDeleted() || !passwordEncoder.matches(userLoginRequest.password(), user.getPassword())) {
      throw new AuthException();
    }
    log.debug("사용자 로그인 성공: userId={}, email={}", user.getId(), user.getEmail());
    return userMapper.toDto(user);
  }

  @Transactional
  public UserDto updateUser(
      UUID userId,
      UserUpdateRequest userUpdateRequest
  ) {
    log.debug("사용자 수정 요청: userId={}",userId);
    User findUser = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    if (findUser.isDeleted()) {
      throw new DeletedUserException(userId);
    }
    Optional.ofNullable(userUpdateRequest.nickname())
        .ifPresent(findUser::updateNickname);
    User updatedUser = userRepository.save(findUser);

    log.debug("사용자 수정 성공: targetUserId={}, newNickname={},", userId, updatedUser.getNickname());
    applicationEventPublisher.publishEvent(
        new UserUpdatedEvent(updatedUser.getId(), updatedUser.getNickname())
    );
    return userMapper.toDto(updatedUser);
  }

  @Transactional
  public void deleteUser(UUID userId) {
    log.debug("사용자 소프트 삭제 시작: userId={}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
    if (user.isDeleted()) {
      throw new DeletedUserException(userId);
    }
    user.markDeleted();
    log.debug("사용자 소프트 삭제 성공: userId={}", userId);
  }

  @Transactional
  public void hardDeleteUser(UUID userId) {
    log.debug("사용자 물리 삭제 시작: userId={}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    // 연관 객체 삭제
    notificationRepository.deleteAllByUserId(userId);
    commentLikeRepository.deleteAllByUserId(userId);
    commentRepository.deleteAllByUserId(userId);
    subscriptionRepository.deleteAllByUserId(userId);

    userRepository.delete(user);
    log.debug("사용자 물리 삭제 성공: userId={}", userId);
    applicationEventPublisher.publishEvent(new UserDeletedEvent(userId));
  }

  private void validateDuplicateEmail(String email) {
    if (userRepository.existsByEmail(email)) {
      throw new DuplicateEmailException();
    }
  }
}

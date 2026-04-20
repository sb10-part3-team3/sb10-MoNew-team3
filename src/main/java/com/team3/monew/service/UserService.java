package com.team3.monew.service;

import com.team3.monew.dto.user.UserLoginRequest;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.dto.user.UserDto;
import com.team3.monew.entity.User;
import com.team3.monew.exception.user.AuthException;
import com.team3.monew.exception.user.DuplicateEmailException;
import com.team3.monew.mapper.UserMapper;
import com.team3.monew.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserDto registerUser(UserRegisterRequest userRegisterRequest) {
    log.debug("사용자 등록 요청: email={}", userRegisterRequest.email());

    validateDuplicateEmail(userRegisterRequest.email());

    User user = userMapper.toEntity(userRegisterRequest);
    String encodedPassword = passwordEncoder.encode(userRegisterRequest.password());
    user.changePassword(encodedPassword);

    // 동시성 문제로 DB레벨 중복 예외 발생 시 예외 전환
    try {
      log.debug("사용자 등록 성공: userId={}, email={}", user.getId(), user.getEmail());
      return userMapper.toDto(userRepository.save(user));
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateEmailException();
    }
  }

  public UserDto loginUser(UserLoginRequest userLoginRequest) {
    log.debug("사용자 로그인 요청: email={},", userLoginRequest.email());
    User user = userRepository.findByEmail(userLoginRequest.email())
        .orElseThrow(AuthException::new);

    if (!passwordEncoder.matches(userLoginRequest.password(), user.getPassword())) {
      throw new AuthException();
    }
    log.debug("사용자 로그인 성공: userId={}, email={}", user.getId(), user.getEmail());
    return userMapper.toDto(user);
  }

  private void validateDuplicateEmail(String email) {
    if (userRepository.existsByEmail(email)) {
      throw new DuplicateEmailException();
    }
  }
}

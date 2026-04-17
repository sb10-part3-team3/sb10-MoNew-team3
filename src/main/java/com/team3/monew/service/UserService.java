package com.team3.monew.service;

import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.dto.user.UserDto;
import com.team3.monew.entity.User;
import com.team3.monew.exception.user.DuplicateEmailException;
import com.team3.monew.mapper.UserMapper;
import com.team3.monew.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserDto registerUser(UserRegisterRequest userRegisterRequest) {

    validateDuplicateEmail(userRegisterRequest.email());

    User user = userMapper.toEntity(userRegisterRequest);
    String encodedPassword = passwordEncoder.encode(userRegisterRequest.password());
    user.changePassword(encodedPassword);

    // 동시성 문제로 DB레벨 중복 예외 발생 시 예외 전환
    try {
      return userMapper.toDto(userRepository.save(user));
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateEmailException();
    }
  }

  private void validateDuplicateEmail(String email) {
    if (userRepository.existsByEmail(email)) {
      throw new DuplicateEmailException();
    }
  }
}

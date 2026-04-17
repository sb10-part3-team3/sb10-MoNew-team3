package com.team3.monew.service;

import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.dto.user.UserDto;
import com.team3.monew.entity.User;
import com.team3.monew.exception.user.DuplicateEmailException;
import com.team3.monew.mapper.UserMapper;
import com.team3.monew.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
  public UserDto createUser(UserRegisterRequest userRegisterRequest) {

    if (userRepository.existsByEmail(userRegisterRequest.email())) {
      throw new DuplicateEmailException(userRegisterRequest.email());
    }

    User user = userMapper.toEntity(userRegisterRequest);
    String encodedPassword = passwordEncoder.encode(userRegisterRequest.password());
    user.changePassword(encodedPassword);

    return userMapper.toDto(userRepository.save(user));
  }
}

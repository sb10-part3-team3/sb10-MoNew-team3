package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team3.monew.dto.user.UserDto;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.entity.User;
import com.team3.monew.exception.user.DuplicateEmailException;
import com.team3.monew.mapper.UserMapper;
import com.team3.monew.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private UserMapper userMapper;
  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("이미 존재하는 이메일이면 DuplicateEmailException을 던진다")
  void shouldThrowDuplicateEmailException_whenEmailAlreadyExists() {
    UserRegisterRequest userRegisterRequest = new UserRegisterRequest(
        "email1@example.com", "username1", "password1"
    );

    given(userRepository.existsByEmail("email1@example.com")).willReturn(true);

    assertThatThrownBy(() -> userService.registerUser(userRegisterRequest)).isInstanceOf(
        DuplicateEmailException.class);
    verify(userRepository).existsByEmail("email1@example.com");
    verify(userRepository, never()).save(any());
    verify(userMapper, never()).toEntity(any());
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("이미 존재하는 이메일로 가입 시 DuplicateEmailException이 발생한다")
  void shouldRegisterUser_whenValidRequest() {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "email1@example.com", "username1", "password1"
    );

    User user = User.create("email1@example.com", "username1", "password1");
    User savedUser = User.create("email1@example.com", "username1", "password1");
    UserDto userDto = new UserDto(UUID.randomUUID(), "email1@example.com", "username1", Instant.now());

    given(userRepository.existsByEmail("email1@example.com")).willReturn(false);
    given(userMapper.toEntity(request)).willReturn(user);
    given(userRepository.save(user)).willReturn(savedUser);
    given(userMapper.toDto(savedUser)).willReturn(userDto);
    given(passwordEncoder.encode("password1")).willReturn("encodedPassword1");

    // when
    UserDto result = userService.registerUser(request);

    // then
    assertThat(result).isEqualTo(userDto);
    verify(userRepository).existsByEmail("email1@example.com");
    verify(userMapper).toEntity(request);
    verify(userRepository).save(user);
    verify(userMapper).toDto(savedUser);
    verify(passwordEncoder).encode("password1");
  }
}
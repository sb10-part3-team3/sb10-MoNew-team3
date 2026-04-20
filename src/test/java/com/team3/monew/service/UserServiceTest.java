package com.team3.monew.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team3.monew.dto.user.UserDto;
import com.team3.monew.dto.user.UserLoginRequest;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.dto.user.UserUpdateRequest;
import com.team3.monew.entity.User;
import com.team3.monew.exception.user.AuthException;
import com.team3.monew.exception.user.DuplicateEmailException;
import com.team3.monew.exception.user.InvalidNicknameException;
import com.team3.monew.exception.user.UserNotFoundException;
import com.team3.monew.global.enums.ErrorCode;
import com.team3.monew.mapper.UserMapper;
import com.team3.monew.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  @DisplayName("유효한 요청으로 회원가입 시 사용자가 정상 생성된다")
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

    ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(savedUserCaptor.capture());
    // capture한 user의 password를 service가 encode 했는지 확인
    assertThat(savedUserCaptor.getValue().getPassword()).isEqualTo("encodedPassword1");

    verify(userMapper).toDto(savedUser);
    verify(passwordEncoder).encode("password1");
  }

  @Test
  @DisplayName("잘못된 비밀번호로 로그인 시 AuthException을 던진다")
  void shouldThrowAuthException_whenInvalidPassword() {
    // given
    String email = "email1@naver.com";
    String rawPassword = "rawPassword";
    String encodedPassword = "encoded-password";

    User user = User.create(email, "username1", encodedPassword);
    UserLoginRequest request = new UserLoginRequest(email, rawPassword);

    given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> userService.loginUser(request))
        .isInstanceOf(AuthException.class);

    verify(userRepository).findByEmail(email);
    verify(passwordEncoder).matches(rawPassword, encodedPassword);
  }

  @Test
  @DisplayName("존재하지 않는 이메일로 로그인 시 인증 예외를 던진다")
  void shouldThrowAuthException_whenNotFoundEmail() {
    // given
    String email = "randomEmail@naver.com";
    UserLoginRequest userLoginRequest = new UserLoginRequest(email, "password1");
    given(userRepository.findByEmail(email)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.loginUser(userLoginRequest))
        .isInstanceOf(AuthException.class);

    verify(userRepository).findByEmail(email);
    verify(passwordEncoder, never()).matches(any(), any());
    verify(userMapper, never()).toDto(any());
  }

  @Test
  @DisplayName("유효한 요청 정보로 로그인 시 dto 반환한다")
  void shouldLoginUser_whenValidRequest() {
    // given
    String email = "email1@naver.com";
    String rawPassword = "rawPassword";
    String encodedPassword = "encoded-password";

    User user = User.create(email, "username1", encodedPassword);
    UserDto userDto = new UserDto(UUID.randomUUID(), email, "username1", Instant.now());
    UserLoginRequest request = new UserLoginRequest(email, rawPassword);

    given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
    given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);
    given(userMapper.toDto(user)).willReturn(userDto);

    // when
    UserDto result = userService.loginUser(request);

    // then
    assertThat(result).isEqualTo(userDto);
    verify(userRepository).findByEmail(email);
    verify(passwordEncoder).matches(rawPassword, encodedPassword);
    verify(userMapper).toDto(user);
  }

  @Test
  @DisplayName("없는 userId로 수정하는 경우 UserNotFoundException을 던진다.")
  void shouldThrowUserNotFoundException_whenNotFoundUser() {
    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("newUsername");

    // empty 유저 반환
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.updateUser(userId, request))
        .isInstanceOf(UserNotFoundException.class);

    verify(userRepository).findById(userId);
    verify(userMapper, never()).toDto(any());
  }


  @Test
  @DisplayName("닉네임이 10자를 초과하면 INVALID_NICKNAME 예외를 던진다.")
  void shouldThrowInvalidNicknameException_whenNicknameLengthExceedsLimit() {
    // given
    UUID userId = UUID.randomUUID();

    User user = User.create("email1@naver.com", "username1", "password1");
    UserUpdateRequest request = new UserUpdateRequest("newUsername"); // 11자

    given(userRepository.findById(userId)).willReturn(Optional.of(user));

    // when & then
    assertThatThrownBy(() -> userService.updateUser(userId, request))
        .isInstanceOfSatisfying(InvalidNicknameException.class, e -> {
          assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_NICKNAME);
        });

    verify(userRepository).findById(userId);
    verify(userRepository, never()).save(any(User.class));
    verify(userMapper, never()).toDto(any(User.class));
  }

  @Test
  @DisplayName("유효한 요청 정보로 수정 시 dto 반환한다.")
  void shouldUpdateUser_whenValidRequest() {
    // given
    UUID userId = UUID.randomUUID();

    User user = User.create("email1@naver.com", "username1", "password1");
    UserUpdateRequest request = new UserUpdateRequest("newname");
    UserDto userDto = new UserDto(userId, "email1@naver.com", "newname", Instant.now());

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(userRepository.save(user)).willReturn(user);
    given(userMapper.toDto(user)).willReturn(userDto);

    // when
    UserDto result = userService.updateUser(userId, request);

    // then
    assertThat(result).isEqualTo(userDto);
    verify(userRepository).findById(userId);
    verify(userMapper).toDto(user);
  }
}
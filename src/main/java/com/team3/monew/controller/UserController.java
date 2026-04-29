package com.team3.monew.controller;

import com.team3.monew.controller.api.UserApi;
import com.team3.monew.dto.user.UserDto;
import com.team3.monew.dto.user.UserLoginRequest;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.dto.user.UserUpdateRequest;
import com.team3.monew.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController implements UserApi {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserDto> registerUser(
      @Valid @RequestBody UserRegisterRequest userRegisterRequest
  ) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(userRegisterRequest));
  }

  @PostMapping("/login")
  public ResponseEntity<UserDto> loginUser(
      @Valid @RequestBody UserLoginRequest userLoginRequest
  ) {
    UserDto userDto = userService.loginUser(userLoginRequest);
    return ResponseEntity.ok(userDto);
  }

  @PatchMapping("/{userId}")
  public ResponseEntity<UserDto> updateUser(
      @PathVariable UUID userId,
      @Valid @RequestBody UserUpdateRequest userUpdateRequest
  ) {
    return ResponseEntity.ok(userService.updateUser(userId, userUpdateRequest));
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
    userService.deleteUser(userId);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{userId}/hard")
  public ResponseEntity<Void> hardDeleteUser(@PathVariable UUID userId) {
    userService.hardDeleteUser(userId);
    return ResponseEntity.ok().build();
  }
}

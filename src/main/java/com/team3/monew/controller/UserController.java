package com.team3.monew.controller;

import com.team3.monew.controller.api.UserApi;
import com.team3.monew.dto.user.UserDto;
import com.team3.monew.dto.user.UserLoginRequest;
import com.team3.monew.dto.user.UserRegisterRequest;
import com.team3.monew.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    return ResponseEntity.ok()
        .header("monew-request-id", userDto.id().toString())
        .body(userDto);
  }
}

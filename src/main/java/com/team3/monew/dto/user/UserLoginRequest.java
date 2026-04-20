package com.team3.monew.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
    @NotBlank
    @Email
    @Size(max = 255, message = "이메일은 255자 이하로 입력해주세요")
    String email,

    @NotBlank
    @Size(min = 6, max = 20, message = "비밀번호는 영문과 숫자, 특수문자를 포함해 6자 이상 입력해 주세요")
    @Pattern(
        message = "비밀번호는 영문과 숫자, 특수문자를 포함해 6자 이상 입력해 주세요",
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+])[A-Za-z\\d!@#$%^&*()_+]+$"
    )
    String password
) {

}

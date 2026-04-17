package com.team3.monew.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(

    @NotBlank
    @Email
    @Size(max = 255)
    String email,

    @NotBlank
    @Size(min = 2, max = 100)
    String nickname,

    @NotBlank
    @Size(min = 8, max = 20)
    @Pattern(
        message = "비밀번호는 영문자와 숫자를 포함해야 하며, 사용할 수 있는 특수문자는 !@#$%^&*()_+ 입니다.",
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+]+$"
    )
    String password
) {

}

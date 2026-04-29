package com.team3.monew.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @NotBlank
    @Size(min = 2, max = 10, message = "닉네임은 2자이상 10자 이하로 입력해주세요")
    String nickname
) {

}

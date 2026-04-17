package com.team3.monew.dto.interest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InterestRegisterRequest(
    @NotBlank
    String name,
    @NotEmpty
    List<@NotBlank String> keywords
) {

}

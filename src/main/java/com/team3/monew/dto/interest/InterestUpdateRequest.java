package com.team3.monew.dto.interest;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record InterestUpdateRequest(
    @NotNull
    @NotEmpty
    List<String> keywords
) {

}

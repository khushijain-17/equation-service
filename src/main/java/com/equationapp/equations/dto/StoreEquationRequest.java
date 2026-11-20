package com.equationapp.equations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreEquationRequest(
        @Schema(example = "3x + 2y - z", description = "Algebraic expression. Supports + - * / ^, parentheses, "
                + "decimals, single-letter variables and implicit multiplication (3x, 2(x+1)).")
        @NotBlank(message = "equation must not be blank")
        @Size(max = 1000, message = "equation must be at most 1000 characters")
        String equation) {
}

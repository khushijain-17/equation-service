package com.equationapp.equations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StoreEquationResponse(
        @Schema(example = "Equation stored successfully") String message,
        @Schema(example = "1") String equationId,
        @Schema(example = "3 x * 2 y * + z -", description = "Postfix (RPN) form the expression tree was built from")
        String postfix) {
}

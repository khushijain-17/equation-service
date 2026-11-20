package com.equationapp.equations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EquationSummary(
        @Schema(example = "1") String equationId,
        @Schema(example = "3x + 2y - z") String equation) {
}

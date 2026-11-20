package com.equationapp.equations.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record EvaluateResponse(
        @Schema(example = "1") String equationId,
        @Schema(example = "3x + 2y - z") String equation,
        Map<String, Number> variables,
        @Schema(example = "11") Number result) {
}

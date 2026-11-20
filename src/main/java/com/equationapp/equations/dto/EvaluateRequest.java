package com.equationapp.equations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record EvaluateRequest(
        @Schema(example = "{\"x\": 2, \"y\": 3, \"z\": 1}", description = "Value for each variable used in the equation")
        Map<String, @NotNull(message = "variable values must not be null") Double> variables) {
}

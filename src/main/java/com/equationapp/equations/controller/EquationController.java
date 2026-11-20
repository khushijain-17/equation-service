package com.equationapp.equations.controller;

import com.equationapp.equations.dto.*;
import com.equationapp.equations.model.Equation;
import com.equationapp.equations.service.EquationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equations")
@Tag(name = "Equations", description = "Store algebraic equations as postfix expression trees and evaluate them")
public class EquationController {

    private final EquationService service;

    public EquationController(EquationService service) {
        this.service = service;
    }

    @PostMapping("/store")
    @Operation(summary = "Store an equation",
            description = "Parses the equation into postfix notation, builds the expression tree and stores it.")
    @ApiResponse(responseCode = "201", description = "Equation stored")
    @ApiResponse(responseCode = "400", description = "Invalid equation syntax",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Storage limit reached",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<StoreEquationResponse> store(@Valid @RequestBody StoreEquationRequest request) {
        Equation saved = service.store(request.equation());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/equations/{id}").buildAndExpand(saved.id()).toUri();
        return ResponseEntity.created(location)
                .body(new StoreEquationResponse("Equation stored successfully", saved.id(), saved.postfix()));
    }

    @GetMapping
    @Operation(summary = "List stored equations",
            description = "Returns every stored equation, reconstructed in infix form by traversing its tree.")
    public EquationListResponse list() {
        List<EquationSummary> items = service.findAll().stream()
                .map(e -> new EquationSummary(e.id(), e.infix()))
                .toList();
        return new EquationListResponse(items);
    }

    @GetMapping("/{equationId}")
    @Operation(summary = "Get one stored equation")
    @ApiResponse(responseCode = "404", description = "Equation not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EquationSummary get(@PathVariable String equationId) {
        Equation e = service.get(equationId);
        return new EquationSummary(e.id(), e.infix());
    }

    @DeleteMapping("/{equationId}")
    @Operation(summary = "Delete a stored equation",
            description = "Removes the equation. Its id is not reused by later equations.")
    @ApiResponse(responseCode = "204", description = "Equation deleted")
    @ApiResponse(responseCode = "404", description = "Equation not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable String equationId) {
        service.delete(equationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{equationId}/evaluate")
    @Operation(summary = "Evaluate an equation",
            description = "Substitutes the supplied variable values into the stored tree and returns the result.")
    @ApiResponse(responseCode = "404", description = "Equation not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Missing variables, division by zero or non-finite result",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EvaluateResponse evaluate(@PathVariable String equationId,
                                     @Valid @RequestBody(required = false) EvaluateRequest request) {
        Map<String, Double> variables = request == null || request.variables() == null
                ? Map.of() : request.variables();
        double result = service.evaluate(equationId, variables);
        Equation e = service.get(equationId);

        Map<String, Number> echoed = new LinkedHashMap<>();
        variables.forEach((k, v) -> echoed.put(k, toNumber(v)));
        return new EvaluateResponse(e.id(), e.infix(), echoed, toNumber(result));
    }

    /** Whole numbers are returned as integers (11, not 11.0). */
    private static Number toNumber(double d) {
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return (long) d;
        }
        return d;
    }
}

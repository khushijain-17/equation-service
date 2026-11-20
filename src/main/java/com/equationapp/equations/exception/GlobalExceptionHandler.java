package com.equationapp.equations.exception;

import com.equationapp.equations.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** Maps exceptions to a consistent JSON error body. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidEquationException.class)
    ResponseEntity<ErrorResponse> invalidEquation(InvalidEquationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid equation", ex.getMessage(), List.of());
    }

    @ExceptionHandler(EquationNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(EquationNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), List.of());
    }

    @ExceptionHandler(StorageLimitExceededException.class)
    ResponseEntity<ErrorResponse> storageLimit(StorageLimitExceededException ex) {
        return build(HttpStatus.CONFLICT, "Storage limit reached", ex.getMessage(), List.of());
    }

    @ExceptionHandler(EvaluationException.class)
    ResponseEntity<ErrorResponse> evaluation(EvaluationException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Evaluation failed", ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e.getDefaultMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", "Request body is invalid", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request",
                "Request body is missing or not valid JSON (variable values must be numbers)", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", ex.getMessage(), List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> mediaType(HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type",
                "Use Content-Type: application/json", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> noResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not found", "No such endpoint", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Unexpected server error", List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), error, message, details));
    }
}

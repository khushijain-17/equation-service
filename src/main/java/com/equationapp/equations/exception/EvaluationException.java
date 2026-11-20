package com.equationapp.equations.exception;

import java.util.List;

/** Thrown when a syntactically valid equation cannot be evaluated (missing variables, divide by zero, ...). */
public class EvaluationException extends RuntimeException {
    private final List<String> details;

    public EvaluationException(String message) {
        this(message, List.of());
    }

    public EvaluationException(String message, List<String> details) {
        super(message);
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}

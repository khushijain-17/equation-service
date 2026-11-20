package com.equationapp.equations.exception;

/** Thrown when an equation string cannot be parsed (bad syntax, unknown characters, ...). */
public class InvalidEquationException extends RuntimeException {
    public InvalidEquationException(String message) {
        super(message);
    }
}

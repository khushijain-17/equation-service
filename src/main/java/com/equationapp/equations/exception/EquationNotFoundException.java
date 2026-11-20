package com.equationapp.equations.exception;

public class EquationNotFoundException extends RuntimeException {
    public EquationNotFoundException(String id) {
        super("Equation with id '" + id + "' not found");
    }
}

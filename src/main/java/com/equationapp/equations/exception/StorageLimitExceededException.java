package com.equationapp.equations.exception;

/** Thrown when the in-memory store already holds the maximum number of equations. */
public class StorageLimitExceededException extends RuntimeException {
    public StorageLimitExceededException(int max) {
        super("Storage limit reached (max " + max + " equations). Delete some equations first.");
    }
}

package com.equationapp.equations.tree;

import java.util.Optional;

/** Binary operators supported in equations, with precedence and associativity. */
public enum Operator {
    ADD('+', 1, false),
    SUBTRACT('-', 1, false),
    MULTIPLY('*', 2, false),
    DIVIDE('/', 2, false),
    POWER('^', 4, true);

    /** Precedence of unary minus: binds tighter than * and /, looser than ^ (so -x^2 = -(x^2)). */
    public static final int UNARY_MINUS_PRECEDENCE = 3;

    private final char symbol;
    private final int precedence;
    private final boolean rightAssociative;

    Operator(char symbol, int precedence, boolean rightAssociative) {
        this.symbol = symbol;
        this.precedence = precedence;
        this.rightAssociative = rightAssociative;
    }

    public char symbol() {
        return symbol;
    }

    public int precedence() {
        return precedence;
    }

    public boolean isRightAssociative() {
        return rightAssociative;
    }

    public static Optional<Operator> fromSymbol(char c) {
        for (Operator op : values()) {
            if (op.symbol == c) {
                return Optional.of(op);
            }
        }
        return Optional.empty();
    }
}

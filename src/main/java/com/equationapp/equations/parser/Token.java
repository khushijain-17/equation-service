package com.equationapp.equations.parser;

import com.equationapp.equations.tree.Operator;

/**
 * Lexical token. {@code position} is the 0-based character offset in the source (for error messages).
 * {@code operator} is only set for {@link TokenType#OPERATOR}.
 */
public record Token(TokenType type, String text, int position, Operator operator) {

    public static Token of(TokenType type, String text, int position) {
        return new Token(type, text, position, null);
    }

    public static Token operator(Operator op, int position) {
        return new Token(TokenType.OPERATOR, String.valueOf(op.symbol()), position, op);
    }

    public static Token unaryMinus(int position) {
        return new Token(TokenType.UNARY_MINUS, "neg", position, null);
    }
}

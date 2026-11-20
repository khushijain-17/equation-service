package com.equationapp.equations.parser;

import com.equationapp.equations.exception.InvalidEquationException;
import com.equationapp.equations.tree.Operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Splits an equation string into tokens.
 * <ul>
 *   <li>Numbers: {@code 3}, {@code 3.5}, {@code .5}</li>
 *   <li>Variables: a single letter ({@code x}, {@code Y}); {@code xy} is two variables, i.e. x*y</li>
 *   <li>Operators: {@code + - * / ^}, parentheses, whitespace is ignored</li>
 * </ul>
 */
public final class Tokenizer {

    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (isDigit(c) || c == '.') {
                int start = i;
                while (i < input.length() && (isDigit(input.charAt(i)) || input.charAt(i) == '.')) {
                    i++;
                }
                String text = input.substring(start, i);
                if (!text.matches("\\d+(\\.\\d+)?|\\.\\d+")) {
                    throw new InvalidEquationException(
                            "Malformed number '" + text + "' at position " + start);
                }
                tokens.add(Token.of(TokenType.NUMBER, text, start));
            } else if (isLetter(c)) {
                tokens.add(Token.of(TokenType.VARIABLE, String.valueOf(c), i));
                i++;
            } else if (c == '(') {
                tokens.add(Token.of(TokenType.LPAREN, "(", i++));
            } else if (c == ')') {
                tokens.add(Token.of(TokenType.RPAREN, ")", i++));
            } else {
                Optional<Operator> op = Operator.fromSymbol(c);
                if (op.isEmpty()) {
                    throw new InvalidEquationException(
                            "Unsupported character '" + c + "' at position " + i);
                }
                tokens.add(Token.operator(op.get(), i++));
            }
        }
        return tokens;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}

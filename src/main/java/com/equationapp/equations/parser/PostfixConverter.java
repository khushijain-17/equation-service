package com.equationapp.equations.parser;

import com.equationapp.equations.exception.InvalidEquationException;
import com.equationapp.equations.tree.Operator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Converts an infix token stream to postfix (Reverse Polish Notation) using the shunting-yard algorithm.
 * Also validates syntax and inserts implicit multiplication ({@code 3x}, {@code 2(x+1)}, {@code (a)(b)}).
 */
public final class PostfixConverter {

    public List<Token> toPostfix(List<Token> tokens) {
        if (tokens.isEmpty()) {
            throw new InvalidEquationException("Equation must not be empty");
        }

        List<Token> output = new ArrayList<>();
        Deque<Token> stack = new ArrayDeque<>();
        boolean expectOperand = true;
        Token previous = null;

        for (Token token : tokens) {
            switch (token.type()) {
                case NUMBER, VARIABLE -> {
                    if (!expectOperand) {
                        if (previous.type() == TokenType.NUMBER && token.type() == TokenType.NUMBER) {
                            throw new InvalidEquationException(
                                    "Missing operator between numbers before position " + token.position());
                        }
                        pushBinary(Token.operator(Operator.MULTIPLY, token.position()), output, stack);
                    }
                    output.add(token);
                    expectOperand = false;
                }
                case LPAREN -> {
                    if (!expectOperand) {
                        pushBinary(Token.operator(Operator.MULTIPLY, token.position()), output, stack);
                    }
                    stack.push(token);
                    expectOperand = true;
                }
                case RPAREN -> {
                    if (expectOperand) {
                        throw new InvalidEquationException(previous != null && previous.type() == TokenType.LPAREN
                                ? "Empty parentheses at position " + previous.position()
                                : "Missing operand before ')' at position " + token.position());
                    }
                    while (!stack.isEmpty() && stack.peek().type() != TokenType.LPAREN) {
                        output.add(stack.pop());
                    }
                    if (stack.isEmpty()) {
                        throw new InvalidEquationException(
                                "Unmatched ')' at position " + token.position());
                    }
                    stack.pop(); // discard '('
                    expectOperand = false;
                }
                case OPERATOR -> {
                    Operator op = token.operator();
                    if (expectOperand) {
                        if (op == Operator.SUBTRACT) {
                            stack.push(Token.unaryMinus(token.position())); // prefix: no popping
                        } else if (op != Operator.ADD) { // unary plus is a no-op
                            throw new InvalidEquationException(
                                    "Operator '" + op.symbol() + "' at position " + token.position()
                                            + " is missing a left operand");
                        }
                    } else {
                        pushBinary(token, output, stack);
                        expectOperand = true;
                    }
                }
                default -> throw new IllegalStateException("Unexpected token " + token);
            }
            previous = token;
        }

        if (expectOperand) {
            throw new InvalidEquationException("Equation ends with an operator or is missing an operand");
        }
        while (!stack.isEmpty()) {
            Token top = stack.pop();
            if (top.type() == TokenType.LPAREN) {
                throw new InvalidEquationException("Unmatched '(' at position " + top.position());
            }
            output.add(top);
        }
        return output;
    }

    private void pushBinary(Token op, List<Token> output, Deque<Token> stack) {
        Operator o1 = op.operator();
        while (!stack.isEmpty() && stack.peek().type() != TokenType.LPAREN) {
            Token top = stack.peek();
            int topPrecedence = top.type() == TokenType.UNARY_MINUS
                    ? Operator.UNARY_MINUS_PRECEDENCE
                    : top.operator().precedence();
            if (topPrecedence > o1.precedence() || (topPrecedence == o1.precedence() && !o1.isRightAssociative())) {
                output.add(stack.pop());
            } else {
                break;
            }
        }
        stack.push(op);
    }

    /** Space separated postfix string, e.g. {@code 3 x * 2 y * + z -}. */
    public static String render(List<Token> postfix) {
        StringBuilder sb = new StringBuilder();
        for (Token t : postfix) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(t.text());
        }
        return sb.toString();
    }
}

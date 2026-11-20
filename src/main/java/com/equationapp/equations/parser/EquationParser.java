package com.equationapp.equations.parser;

import com.equationapp.equations.exception.InvalidEquationException;
import com.equationapp.equations.tree.ExpressionNode;
import org.springframework.stereotype.Component;

import java.util.List;

/** Facade over the pipeline: text -> normalized text -> tokens -> postfix -> expression tree. */
@Component
public class EquationParser {

    public static final int MAX_LENGTH = 1000;

    private final Tokenizer tokenizer = new Tokenizer();
    private final PostfixConverter postfixConverter = new PostfixConverter();
    private final ExpressionTreeBuilder treeBuilder = new ExpressionTreeBuilder();

    public record ParsedEquation(ExpressionNode root, String postfix) {}

    public ParsedEquation parse(String equation) {
        if (equation == null || equation.isBlank()) {
            throw new InvalidEquationException("Equation must not be empty");
        }
        if (equation.length() > MAX_LENGTH) {
            throw new InvalidEquationException("Equation is too long (max " + MAX_LENGTH + " characters)");
        }
        String normalized = InputNormalizer.normalize(equation);
        if (normalized.isBlank()) {
            throw new InvalidEquationException("Equation must not be empty");
        }
        if (normalized.indexOf('=') >= 0) {
            throw new InvalidEquationException(
                    "Only expressions are supported (no '='); store the left-hand side, e.g. 'x^2 - 4'");
        }
        List<Token> postfix = postfixConverter.toPostfix(tokenizer.tokenize(normalized));
        return new ParsedEquation(treeBuilder.build(postfix), PostfixConverter.render(postfix));
    }
}

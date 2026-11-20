package com.equationapp.equations.parser;

import com.equationapp.equations.exception.InvalidEquationException;
import com.equationapp.equations.tree.ExpressionNode;
import com.equationapp.equations.tree.ExpressionNode.BinaryNode;
import com.equationapp.equations.tree.ExpressionNode.NumberNode;
import com.equationapp.equations.tree.ExpressionNode.UnaryMinusNode;
import com.equationapp.equations.tree.ExpressionNode.VariableNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Builds the expression tree from a postfix token list using an operand stack. */
public final class ExpressionTreeBuilder {

    public ExpressionNode build(List<Token> postfix) {
        Deque<ExpressionNode> stack = new ArrayDeque<>();
        for (Token t : postfix) {
            switch (t.type()) {
                case NUMBER -> stack.push(new NumberNode(Double.parseDouble(t.text())));
                case VARIABLE -> stack.push(new VariableNode(t.text()));
                case UNARY_MINUS -> {
                    requireOperands(stack, 1, t);
                    stack.push(new UnaryMinusNode(stack.pop()));
                }
                case OPERATOR -> {
                    requireOperands(stack, 2, t);
                    ExpressionNode right = stack.pop();
                    ExpressionNode left = stack.pop();
                    stack.push(new BinaryNode(t.operator(), left, right));
                }
                default -> throw new InvalidEquationException("Unexpected token '" + t.text() + "' in postfix");
            }
        }
        if (stack.size() != 1) {
            throw new InvalidEquationException("Malformed equation: operands and operators do not balance");
        }
        return stack.pop();
    }

    private static void requireOperands(Deque<ExpressionNode> stack, int n, Token t) {
        if (stack.size() < n) {
            throw new InvalidEquationException(
                    "Operator '" + t.text() + "' at position " + t.position() + " is missing an operand");
        }
    }
}

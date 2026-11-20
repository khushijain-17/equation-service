package com.equationapp.equations.tree;

import com.equationapp.equations.tree.ExpressionNode.BinaryNode;
import com.equationapp.equations.tree.ExpressionNode.NumberNode;
import com.equationapp.equations.tree.ExpressionNode.UnaryMinusNode;
import com.equationapp.equations.tree.ExpressionNode.VariableNode;

/**
 * Reconstructs a readable infix string from the tree (in-order traversal), adding parentheses
 * only where precedence/associativity require them. {@code 3*x} is rendered as {@code 3x}.
 */
public final class InfixFormatter {

    private InfixFormatter() {}

    public static String format(ExpressionNode node) {
        return switch (node) {
            case NumberNode n -> formatNumber(n.value());
            case VariableNode v -> v.name();
            case UnaryMinusNode u -> "-" + wrapIf(u.operand(), precedence(u.operand()) < Operator.UNARY_MINUS_PRECEDENCE);
            case BinaryNode b -> formatBinary(b);
        };
    }

    private static String formatBinary(BinaryNode b) {
        Operator op = b.operator();
        boolean leftParens = precedence(b.left()) < op.precedence()
                || (precedence(b.left()) == op.precedence() && op.isRightAssociative());
        boolean rightParens = precedence(b.right()) < op.precedence()
                || (precedence(b.right()) == op.precedence() && !op.isRightAssociative());

        String left = wrapIf(b.left(), leftParens);
        String right = wrapIf(b.right(), rightParens);

        if (op == Operator.POWER) {
            return left + "^" + right;
        }
        if (op == Operator.MULTIPLY && isImplicitProduct(b, rightParens)) {
            return left + right;
        }
        return left + " " + op.symbol() + " " + right;
    }

    /** 3*x, 2*x^2 are shown as 3x, 2x^2. */
    private static boolean isImplicitProduct(BinaryNode b, boolean rightParens) {
        return !rightParens && b.left() instanceof NumberNode && leftmostLeaf(b.right()) instanceof VariableNode
                && (b.right() instanceof VariableNode
                || (b.right() instanceof BinaryNode r && r.operator() == Operator.POWER));
    }

    private static ExpressionNode leftmostLeaf(ExpressionNode node) {
        ExpressionNode current = node;
        while (current instanceof BinaryNode b && b.operator() == Operator.POWER) {
            current = b.left();
        }
        return current;
    }

    private static int precedence(ExpressionNode node) {
        return switch (node) {
            case NumberNode n -> Integer.MAX_VALUE;
            case VariableNode v -> Integer.MAX_VALUE;
            case UnaryMinusNode u -> Operator.UNARY_MINUS_PRECEDENCE;
            case BinaryNode b -> b.operator().precedence();
        };
    }

    private static String wrapIf(ExpressionNode node, boolean wrap) {
        String s = format(node);
        return wrap ? "(" + s + ")" : s;
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}

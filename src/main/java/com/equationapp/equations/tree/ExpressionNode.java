package com.equationapp.equations.tree;

/**
 * Node of the postfix expression tree. Operators are parent nodes, operands are leaves/children.
 * Sealed so that evaluator/formatter switches are checked exhaustively by the compiler.
 */
public sealed interface ExpressionNode {

    record NumberNode(double value) implements ExpressionNode {}

    record VariableNode(String name) implements ExpressionNode {}

    record UnaryMinusNode(ExpressionNode operand) implements ExpressionNode {}

    record BinaryNode(Operator operator, ExpressionNode left, ExpressionNode right) implements ExpressionNode {}
}

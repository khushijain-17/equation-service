package com.equationapp.equations.tree;

import com.equationapp.equations.exception.EvaluationException;
import com.equationapp.equations.tree.ExpressionNode.BinaryNode;
import com.equationapp.equations.tree.ExpressionNode.NumberNode;
import com.equationapp.equations.tree.ExpressionNode.UnaryMinusNode;
import com.equationapp.equations.tree.ExpressionNode.VariableNode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Evaluates an expression tree (post-order traversal) and collects the variables it uses. */
public final class ExpressionEvaluator {

    /**
     * A double carries ~15.95 significant decimal digits. Rounding the final result to 15 removes binary
     * floating-point noise (0.1 + 0.2 = 0.30000000000000004 -> 0.3) without losing real precision.
     */
    private static final MathContext RESULT_PRECISION = new MathContext(15, RoundingMode.HALF_EVEN);

    private ExpressionEvaluator() {}

    public static double evaluate(ExpressionNode root, Map<String, Double> variables) {
        List<String> missing = variablesOf(root).stream().filter(v -> !variables.containsKey(v)).toList();
        if (!missing.isEmpty()) {
            throw new EvaluationException("Missing values for variables: " + String.join(", ", missing), missing);
        }
        double result = eval(root, variables);
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new EvaluationException("Result is not a finite number (overflow or undefined operation)");
        }
        return new BigDecimal(result).round(RESULT_PRECISION).doubleValue();
    }

    /** Distinct variable names used by the tree, sorted alphabetically. */
    public static Set<String> variablesOf(ExpressionNode root) {
        Set<String> names = new TreeSet<>();
        collect(root, names);
        return names;
    }

    private static void collect(ExpressionNode node, Set<String> names) {
        switch (node) {
            case NumberNode n -> { }
            case VariableNode v -> names.add(v.name());
            case UnaryMinusNode u -> collect(u.operand(), names);
            case BinaryNode b -> {
                collect(b.left(), names);
                collect(b.right(), names);
            }
        }
    }

    private static double eval(ExpressionNode node, Map<String, Double> variables) {
        return switch (node) {
            case NumberNode n -> n.value();
            case VariableNode v -> variables.get(v.name());
            case UnaryMinusNode u -> -eval(u.operand(), variables);
            case BinaryNode b -> {
                double l = eval(b.left(), variables);
                double r = eval(b.right(), variables);
                yield switch (b.operator()) {
                    case ADD -> l + r;
                    case SUBTRACT -> l - r;
                    case MULTIPLY -> l * r;
                    case DIVIDE -> {
                        if (r == 0) {
                            throw new EvaluationException("Division by zero");
                        }
                        yield l / r;
                    }
                    case POWER -> {
                        double p = Math.pow(l, r);
                        if (Double.isNaN(p)) {
                            throw new EvaluationException("Undefined power: " + l + " ^ " + r);
                        }
                        yield p;
                    }
                };
            }
        };
    }
}

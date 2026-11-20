package com.equationapp.equations.tree;

import com.equationapp.equations.exception.EvaluationException;
import com.equationapp.equations.parser.EquationParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionEvaluatorTest {

    private final EquationParser parser = new EquationParser();

    private double eval(String eq, Map<String, Double> vars) {
        return ExpressionEvaluator.evaluate(parser.parse(eq).root(), vars);
    }

    @Test
    void evaluatesTheExampleFromTheAssignment() {
        // NB: the assignment text says 10, but 3*2 + 2*3 - 1 = 11.
        assertThat(eval("3x + 2y - z", Map.of("x", 2.0, "y", 3.0, "z", 1.0))).isEqualTo(11.0);
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = '|', value = {
            "1 + 2 * 3                    | 7",
            "(1 + 2) * 3                  | 9",
            "2 ^ 3 ^ 2                    | 512",
            "(2 ^ 3) ^ 2                  | 64",
            "10 - 4 - 3                   | 3",
            "100 / 10 / 5                 | 2",
            "-2 ^ 2                       | -4",
            "(-2) ^ 2                     | 4",
            "2 ^ -1                       | 0.5",
            "- - 3                        | 3",
            "3 - -3                       | 6",
            "2(3 + 4)                     | 14",
            "(1 + 1)(2 + 2)               | 8",
            "0.1 * 10                     | 1",
            "7 / 2                        | 3.5",
            "2 * 3 ^ 2                    | 18",
            "4 ^ 0.5                      | 2"
    })
    void respectsPrecedenceAndAssociativity(String eq, double expected) {
        assertThat(eval(eq, Map.of())).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} = {1} (no floating-point noise)")
    @CsvSource(delimiter = '|', value = {
            "0.1 + 0.2                | 0.3",
            "0.1 * 3                  | 0.3",
            "4.35 * 100               | 435",
            "1.1 * 1.1                | 1.21",
            "1 / 3 * 3                | 1",
            "0.7 + 0.1                | 0.8",
            "3 * 1.1                  | 3.3",
            "123456789012345 + 0      | 123456789012345"
    })
    void removesFloatingPointNoise(String eq, double expected) {
        assertThat(eval(eq, Map.of())).isEqualTo(expected);
    }

    @Test
    void keepsFifteenSignificantDigitsForNonTerminatingResults() {
        assertThat(eval("1 / 3", Map.of())).isEqualTo(0.333333333333333);
        assertThat(eval("2 ^ 0.5", Map.of())).isEqualTo(1.41421356237310);
    }

    @Test
    void substitutesVariables() {
        assertThat(eval("x^2 + y^2 - 4", Map.of("x", 3.0, "y", 4.0))).isEqualTo(21.0);
        assertThat(eval("2x(x + 1)", Map.of("x", 3.0))).isEqualTo(24.0);
        assertThat(eval("x - y", Map.of("x", -1.0, "y", -1.0))).isZero();
        assertThat(eval("x / y", Map.of("x", 1.0, "y", 4.0))).isEqualTo(0.25);
    }

    @Test
    void variablesAreCaseSensitive() {
        assertThat(eval("x + X", Map.of("x", 1.0, "X", 10.0))).isEqualTo(11.0);
        assertThatThrownBy(() -> eval("x + X", Map.of("x", 1.0))).isInstanceOf(EvaluationException.class);
    }

    @Test
    void extraVariablesAreIgnored() {
        assertThat(eval("x", Map.of("x", 5.0, "unused", 9.0))).isEqualTo(5.0);
    }

    @Test
    void reportsAllMissingVariablesSorted() {
        assertThatThrownBy(() -> eval("z + x + y", Map.of("y", 1.0)))
                .isInstanceOfSatisfying(EvaluationException.class, ex -> {
                    assertThat(ex.getMessage()).contains("x, z");
                    assertThat(ex.getDetails()).containsExactly("x", "z");
                });
    }

    @Test
    void rejectsDivisionByZero() {
        assertThatThrownBy(() -> eval("1 / (x - x)", Map.of("x", 2.0)))
                .isInstanceOf(EvaluationException.class).hasMessageContaining("Division by zero");
    }

    @Test
    void rejectsUndefinedAndOverflowingResults() {
        assertThatThrownBy(() -> eval("(-8) ^ 0.5", Map.of())).isInstanceOf(EvaluationException.class);
        assertThatThrownBy(() -> eval("10 ^ 400", Map.of()))
                .isInstanceOf(EvaluationException.class).hasMessageContaining("finite");
    }

    @Test
    void collectsDistinctVariables() {
        assertThat(ExpressionEvaluator.variablesOf(parser.parse("z*x + x*y - 3").root()))
                .containsExactly("x", "y", "z");
    }
}

package com.equationapp.equations.parser;

import com.equationapp.equations.exception.InvalidEquationException;
import com.equationapp.equations.tree.InfixFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquationParserTest {

    private final EquationParser parser = new EquationParser();

    @ParameterizedTest(name = "{0}  ->  postfix [{1}]")
    @CsvSource(delimiter = '|', value = {
            "3x + 2y - z            | 3 x * 2 y * + z -",
            "x^2 + y^2 - 4          | x 2 ^ y 2 ^ + 4 -",
            "1 + 2 * 3              | 1 2 3 * +",
            "(1 + 2) * 3            | 1 2 + 3 *",
            "2 ^ 3 ^ 2              | 2 3 2 ^ ^",
            "10 - 4 - 3             | 10 4 - 3 -",
            "-x^2                   | x 2 ^ neg",
            "-2 * 3                 | 2 neg 3 *",
            "2 ^ -3                 | 2 3 neg ^",
            "2(x + 1)               | 2 x 1 + *",
            "(a)(b)                 | a b *",
            "xy                     | x y *",
            "+5                     | 5",
            "1.5x                   | 1.5 x *"
    })
    void convertsToPostfix(String infix, String expectedPostfix) {
        assertThat(parser.parse(infix).postfix()).isEqualTo(expectedPostfix);
    }

    @ParameterizedTest(name = "{0}  ->  \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "3x + 2y - z          | 3x + 2y - z",
            "x^2 + y^2 - 4        | x^2 + y^2 - 4",
            "3*x+2*y-z            | 3x + 2y - z",
            "((x))                | x",
            "(x + y) * 2          | (x + y) * 2",
            "a - (b - c)          | a - (b - c)",
            "a - b - c            | a - b - c",
            "a / (b * c)          | a / (b * c)",
            "(a + b)^2            | (a + b)^2",
            "-x^2                 | -x^2",
            "(-x)^2               | (-x)^2",
            "2^3^2                | 2^3^2",
            "(2^3)^2              | (2^3)^2",
            "2.50 * 4             | 2.5 * 4"
    })
    void reconstructsInfixFromTree(String input, String expected) {
        assertThat(InfixFormatter.format(parser.parse(input).root())).isEqualTo(expected);
    }

    @Test
    void reconstructedInfixReparsesToSameTree() {
        String[] samples = {"3x + 2y - z", "-(x + 1)^2 / (y - 2)", "2(x + 1)(x - 1)", "a - (b - c) * -d"};
        for (String s : samples) {
            var first = parser.parse(s);
            var second = parser.parse(InfixFormatter.format(first.root()));
            assertThat(second.root()).as(s).isEqualTo(first.root());
        }
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @ValueSource(strings = {
            "", "   ", "3 +", "+ * 3", "* 3", "3 4", "(3 + 4", "3 + 4)", "()", "(-)", "3 + * 4", "3 $ 4",
            "1..2 + x", "x = 3", "3 +* 4", "x y +", "sin(x)2 +"
    })
    void rejectsInvalidEquations(String bad) {
        assertThatThrownBy(() -> parser.parse(bad)).isInstanceOf(InvalidEquationException.class);
    }

    @Test
    void errorMessagesPointAtTheProblem() {
        assertThatThrownBy(() -> parser.parse("3 $ 4")).hasMessageContaining("'$'").hasMessageContaining("position 2");
        assertThatThrownBy(() -> parser.parse("(3 + 4")).hasMessageContaining("Unmatched '('");
        assertThatThrownBy(() -> parser.parse("3 + 4)")).hasMessageContaining("Unmatched ')'");
        assertThatThrownBy(() -> parser.parse("3 4")).hasMessageContaining("Missing operator");
        assertThatThrownBy(() -> parser.parse("x = 3")).hasMessageContaining("'='");
    }

    @ParameterizedTest(name = "pasted \"{0}\"  ->  \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "3×x − 2÷y            | 3x - 2 / y",
            "x² + y² − 4          | x^2 + y^2 - 4",
            "x³                   | x^3",
            "2·x                  | 2x",
            "x**2 + 1             | x^2 + 1",
            "2**3**2              | 2^3^2",
            "5 – 3                | 5 - 3",
            "5 — 3                | 5 - 3"
    })
    void acceptsSymbolsPastedFromOtherTools(String pasted, String expectedInfix) {
        assertThat(InfixFormatter.format(parser.parse(pasted).root())).isEqualTo(expectedInfix);
    }

    @Test
    void treatsNonBreakingAndThinSpacesAsSpaces() {
        assertThat(parser.parse("3 + 4").postfix()).isEqualTo("3 4 +");
        assertThat(parser.parse("x​+　y").postfix()).isEqualTo("x y +");
    }

    @Test
    void stillRejectsTripleStarAndOnlyInvisibleCharacters() {
        assertThatThrownBy(() -> parser.parse("x***2")).isInstanceOf(InvalidEquationException.class);
        assertThatThrownBy(() -> parser.parse(" ​")).isInstanceOf(InvalidEquationException.class);
    }

    @Test
    void nullAndOverlongInputRejected() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(InvalidEquationException.class);
        assertThatThrownBy(() -> parser.parse("x+".repeat(600) + "x"))
                .isInstanceOf(InvalidEquationException.class).hasMessageContaining("too long");
    }

    @Test
    void deeplyNestedButWithinLimitStillParses() {
        String nested = "(".repeat(400) + "x" + ")".repeat(400);
        assertThat(parser.parse(nested).postfix()).isEqualTo("x");
    }
}

package com.equationapp.equations.parser;

/**
 * Cleans up equations typed or pasted from other tools (Word, phones, web pages) before tokenizing:
 * <ul>
 *   <li>{@code × ·} → {@code *}, {@code ÷} → {@code /}, {@code − – —} → {@code -}</li>
 *   <li>{@code **} (Python style) → {@code ^}</li>
 *   <li>superscripts {@code ² ³} → {@code ^2 ^3}</li>
 *   <li>non-breaking / thin / zero-width / full-width spaces → a normal space</li>
 * </ul>
 */
public final class InputNormalizer {

    private InputNormalizer() {}

    public static String normalize(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '×', '·', '⋅' -> sb.append('*');          // × · ⋅
                case '÷' -> sb.append('/');                              // ÷
                case '−', '–', '—' -> sb.append('-');          // − – —
                case '²' -> sb.append("^2");                             // ²
                case '³' -> sb.append("^3");                             // ³
                case ' ', ' ', ' ', '​', ' ', '　' -> sb.append(' ');
                case '*' -> {
                    if (i + 1 < input.length() && input.charAt(i + 1) == '*') {
                        sb.append('^');
                        i++; // consume the second '*'
                    } else {
                        sb.append('*');
                    }
                }
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}

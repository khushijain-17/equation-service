package com.equationapp.equations.model;

import com.equationapp.equations.tree.ExpressionNode;
import com.equationapp.equations.tree.InfixFormatter;

/** A stored equation: its expression tree plus the postfix form it was built from. */
public record Equation(String id, ExpressionNode root, String postfix) {

    public Equation withId(String newId) {
        return new Equation(newId, root, postfix);
    }

    /** Infix representation reconstructed from the tree. */
    public String infix() {
        return InfixFormatter.format(root);
    }
}

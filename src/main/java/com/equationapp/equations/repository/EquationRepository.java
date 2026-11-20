package com.equationapp.equations.repository;

import com.equationapp.equations.model.Equation;

import java.util.List;
import java.util.Optional;

/** Storage abstraction so the in-memory store can later be swapped for a database. */
public interface EquationRepository {

    /** Persists the equation, assigning a new id if {@code equation.id()} is null. */
    Equation save(Equation equation);

    Optional<Equation> findById(String id);

    /** All equations in insertion order. */
    List<Equation> findAll();

    /** Removes the equation; returns false if no equation had that id. Ids are never reused. */
    boolean deleteById(String id);

    int count();
}

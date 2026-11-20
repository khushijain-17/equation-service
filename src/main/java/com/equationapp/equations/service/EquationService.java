package com.equationapp.equations.service;

import com.equationapp.equations.exception.EquationNotFoundException;
import com.equationapp.equations.exception.StorageLimitExceededException;
import com.equationapp.equations.model.Equation;
import com.equationapp.equations.parser.EquationParser;
import com.equationapp.equations.parser.EquationParser.ParsedEquation;
import com.equationapp.equations.repository.EquationRepository;
import com.equationapp.equations.tree.ExpressionEvaluator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EquationService {

    private final EquationParser parser;
    private final EquationRepository repository;
    private final int maxStored;

    public EquationService(EquationParser parser,
                           EquationRepository repository,
                           @Value("${equations.max-stored:1000}") int maxStored) {
        this.parser = parser;
        this.repository = repository;
        this.maxStored = maxStored;
    }

    /** Synchronized so the "is there room?" check and the save are atomic. */
    public synchronized Equation store(String equationText) {
        ParsedEquation parsed = parser.parse(equationText); // syntax errors win over the limit error
        if (repository.count() >= maxStored) {
            throw new StorageLimitExceededException(maxStored);
        }
        return repository.save(new Equation(null, parsed.root(), parsed.postfix()));
    }

    public List<Equation> findAll() {
        return repository.findAll();
    }

    public Equation get(String id) {
        return repository.findById(id).orElseThrow(() -> new EquationNotFoundException(id));
    }

    public void delete(String id) {
        if (!repository.deleteById(id)) {
            throw new EquationNotFoundException(id);
        }
    }

    public double evaluate(String id, Map<String, Double> variables) {
        Equation equation = get(id);
        return ExpressionEvaluator.evaluate(equation.root(), variables == null ? Map.of() : variables);
    }
}

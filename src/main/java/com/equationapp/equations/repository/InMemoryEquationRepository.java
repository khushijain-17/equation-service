package com.equationapp.equations.repository;

import com.equationapp.equations.model.Equation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe in-memory implementation; ids are sequential and listing keeps insertion order. */
@Repository
public class InMemoryEquationRepository implements EquationRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentNavigableMap<Long, Equation> store = new ConcurrentSkipListMap<>();

    @Override
    public Equation save(Equation equation) {
        long key = equation.id() == null ? sequence.incrementAndGet() : Long.parseLong(equation.id());
        Equation saved = equation.withId(Long.toString(key));
        store.put(key, saved);
        return saved;
    }

    @Override
    public Optional<Equation> findById(String id) {
        Long key = parse(id);
        return key == null ? Optional.empty() : Optional.ofNullable(store.get(key));
    }

    @Override
    public List<Equation> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public boolean deleteById(String id) {
        Long key = parse(id);
        return key != null && store.remove(key) != null;
    }

    @Override
    public int count() {
        return store.size();
    }

    private static Long parse(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

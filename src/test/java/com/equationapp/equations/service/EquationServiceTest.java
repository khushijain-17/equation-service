package com.equationapp.equations.service;

import com.equationapp.equations.exception.EquationNotFoundException;
import com.equationapp.equations.exception.InvalidEquationException;
import com.equationapp.equations.exception.StorageLimitExceededException;
import com.equationapp.equations.model.Equation;
import com.equationapp.equations.parser.EquationParser;
import com.equationapp.equations.repository.InMemoryEquationRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquationServiceTest {

    private final EquationService service =
            new EquationService(new EquationParser(), new InMemoryEquationRepository(), 1000);

    @Test
    void storesWithSequentialIdsAndListsInInsertionOrder() {
        Equation a = service.store("3x + 2y - z");
        Equation b = service.store("x^2 + y^2 - 4");

        assertThat(a.id()).isEqualTo("1");
        assertThat(b.id()).isEqualTo("2");
        assertThat(a.postfix()).isEqualTo("3 x * 2 y * + z -");
        assertThat(service.findAll()).extracting(Equation::infix)
                .containsExactly("3x + 2y - z", "x^2 + y^2 - 4");
    }

    @Test
    void invalidEquationIsNotStored() {
        assertThatThrownBy(() -> service.store("3 +")).isInstanceOf(InvalidEquationException.class);
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void evaluatesStoredEquation() {
        String id = service.store("3x + 2y - z").id();
        assertThat(service.evaluate(id, Map.of("x", 2.0, "y", 3.0, "z", 1.0))).isEqualTo(11.0);
    }

    @Test
    void unknownOrNonNumericIdIsNotFound() {
        assertThatThrownBy(() -> service.get("99")).isInstanceOf(EquationNotFoundException.class);
        assertThatThrownBy(() -> service.get("abc")).isInstanceOf(EquationNotFoundException.class);
    }

    @Test
    void deleteRemovesEquationAndIdsAreNotReused() {
        String first = service.store("x + 1").id();
        service.store("x + 2");

        service.delete(first);

        assertThatThrownBy(() -> service.get(first)).isInstanceOf(EquationNotFoundException.class);
        assertThat(service.findAll()).extracting(Equation::infix).containsExactly("x + 2");
        assertThat(service.store("x + 3").id()).isEqualTo("3"); // not "1" or "2"
    }

    @Test
    void deletingUnknownOrAlreadyDeletedIdIsNotFound() {
        String id = service.store("x").id();
        service.delete(id);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(EquationNotFoundException.class);
        assertThatThrownBy(() -> service.delete("abc")).isInstanceOf(EquationNotFoundException.class);
    }

    @Test
    void storageLimitIsEnforcedAndFreedByDeleting() {
        EquationService small = new EquationService(new EquationParser(), new InMemoryEquationRepository(), 2);
        String a = small.store("a").id();
        small.store("b");

        assertThatThrownBy(() -> small.store("c"))
                .isInstanceOf(StorageLimitExceededException.class).hasMessageContaining("max 2");

        small.delete(a);
        assertThat(small.store("c").id()).isEqualTo("3");
    }

    @Test
    void syntaxErrorsAreReportedEvenWhenStorageIsFull() {
        EquationService full = new EquationService(new EquationParser(), new InMemoryEquationRepository(), 1);
        full.store("a");
        assertThatThrownBy(() -> full.store("3 +")).isInstanceOf(InvalidEquationException.class);
    }

    @Test
    void concurrentStoresGetUniqueIds() throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            var futures = IntStream.range(0, 200).mapToObj(i -> pool.submit(() -> service.store("x + " + i))).toList();
            for (var f : futures) {
                f.get();
            }
        }
        assertThat(service.findAll()).hasSize(200).extracting(Equation::id).doesNotHaveDuplicates();
    }
}

package ru.practicum.ewm.stats.aggregator;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.*;
import ru.practicum.ewm.stats.kafka.ActionWeight;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SimilarityCalculatorTest {
    @Test
    void incrementalResultsMatchFullCalculationForRandomActions() {
        var calculator = new SimilarityCalculator();
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        Map<String, Double> published = new HashMap<>();
        Random random = new Random(17);
        for (int step = 0; step < 400; step++) {
            long user = random.nextInt(8) + 1;
            long event = random.nextInt(6) + 1;
            var type = ActionTypeAvro.values()[random.nextInt(3)];
            var timestamp = Instant.ofEpochMilli(step);
            matrix.computeIfAbsent(event, ignored -> new HashMap<>()).merge(user, ActionWeight.of(type), Math::max);
            calculator.accept(new UserActionAvro(user, event, type, timestamp), similarity -> {
                assertTrue(similarity.getEventA() < similarity.getEventB());
                assertEquals(timestamp, similarity.getTimestamp());
                published.put(similarity.getEventA() + ":" + similarity.getEventB(), similarity.getScore());
            });
            for (long a : matrix.keySet()) {
                for (long b : matrix.keySet()) {
                    if (a >= b) {
                        continue;
                    }
                    var first = matrix.get(a);
                    var second = matrix.get(b);
                    double minimum = first.entrySet().stream()
                            .mapToDouble(entry -> Math.min(entry.getValue(), second.getOrDefault(entry.getKey(), 0.0))).sum();
                    double expected = minimum / Math.sqrt(first.values().stream().mapToDouble(Double::doubleValue).sum()
                            * second.values().stream().mapToDouble(Double::doubleValue).sum());
                    assertEquals(expected, published.get(a + ":" + b), 1e-12);
                }
            }
        }
    }

    @Test
    void failedPublishCanBeRetriedAndLowerWeightDoesNotRecalculate() {
        var calculator = new SimilarityCalculator();
        calculator.accept(new UserActionAvro(1L, 9L, ActionTypeAvro.LIKE, Instant.EPOCH), ignored -> { });
        var action = new UserActionAvro(1L, 2L, ActionTypeAvro.REGISTER, Instant.EPOCH);
        assertThrows(IllegalStateException.class, () -> calculator.accept(action, ignored -> {
            throw new IllegalStateException("Kafka unavailable");
        }));
        List<EventSimilarityAvro> result = new ArrayList<>();
        calculator.accept(action, result::add);
        assertEquals(1, result.size());
        assertEquals(2L, result.getFirst().getEventA());
        assertEquals(0.8 / Math.sqrt(0.8), result.getFirst().getScore(), 1e-12);
        calculator.accept(new UserActionAvro(1L, 2L, ActionTypeAvro.VIEW, Instant.EPOCH), result::add);
        calculator.accept(action, result::add);
        assertEquals(1, result.size());
        calculator.reset();
        calculator.accept(action, result::add);
        assertEquals(1, result.size());
    }
}

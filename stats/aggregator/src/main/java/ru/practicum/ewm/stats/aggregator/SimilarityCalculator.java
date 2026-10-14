package ru.practicum.ewm.stats.aggregator;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.kafka.ActionWeight;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class SimilarityCalculator {
    private final Map<Long, Map<Long, Double>> weights = new HashMap<>();
    private final Map<Long, Double> totals = new HashMap<>();
    private final Map<Pair, Double> minimums = new HashMap<>();

    private record Pair(long first, long second) {
        static Pair of(long a, long b) {
            return new Pair(Math.min(a, b), Math.max(a, b));
        }
    }

    public synchronized void reset() {
        weights.clear();
        totals.clear();
        minimums.clear();
    }

    public synchronized void accept(UserActionAvro action, Consumer<EventSimilarityAvro> publish) {
        long event = action.getEventId();
        long user = action.getUserId();
        double weight = ActionWeight.of(action.getActionType());
        double previous = weights.getOrDefault(event, Map.of()).getOrDefault(user, 0.0);
        if (weight <= previous) {
            return;
        }
        double total = totals.getOrDefault(event, 0.0) + weight - previous;
        Map<Pair, Double> updated = new HashMap<>();
        for (var entry : weights.entrySet()) {
            long other = entry.getKey();
            if (other == event) {
                continue;
            }
            Pair pair = Pair.of(event, other);
            double otherWeight = entry.getValue().getOrDefault(user, 0.0);
            double minimum = minimums.getOrDefault(pair, 0.0)
                    + Math.min(weight, otherWeight) - Math.min(previous, otherWeight);
            double score = minimum / Math.sqrt(total * totals.get(other));
            updated.put(pair, minimum);
            publish.accept(new EventSimilarityAvro(pair.first(), pair.second(), score, action.getTimestamp()));
        }
        // Commit state only after every output was acknowledged; retries reproduce all outputs.
        weights.computeIfAbsent(event, ignored -> new HashMap<>()).put(user, weight);
        totals.put(event, total);
        minimums.putAll(updated);
    }
}

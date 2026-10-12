package ru.practicum.ewm.stats.analyzer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecommendationService {
    private final RecommendationStore store;
    private final int neighbors;

    public RecommendationService(RecommendationStore store, @Value("${stats.recommendations.neighbors:10}") int neighbors) {
        this.store = store;
        if (neighbors < 1) {
            throw new IllegalArgumentException("Neighbor count must be positive");
        }
        this.neighbors = neighbors;
    }

    public record Recommendation(long eventId, double score) {
    }

    private static final Comparator<Recommendation> ORDER = Comparator.comparingDouble(Recommendation::score)
            .reversed().thenComparingLong(Recommendation::eventId);

    public List<Recommendation> similar(long eventId, long userId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        var seen = store.history(userId).stream().map(RecommendationStore.Interaction::eventId).collect(Collectors.toSet());
        return store.similar(eventId).stream()
                .map(pair -> new Recommendation(pair.other(eventId), pair.score()))
                .filter(item -> !seen.contains(item.eventId()))
                .sorted(ORDER).limit(limit).toList();
    }

    public List<Recommendation> recommend(long userId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        var history = store.history(userId);
        if (history.isEmpty()) {
            return List.of();
        }
        var weights = history.stream().collect(Collectors.toMap(RecommendationStore.Interaction::eventId,
                RecommendationStore.Interaction::weight));
        Map<Long, Double> candidates = new HashMap<>();
        history.stream().limit(limit).forEach(interaction ->
                store.similar(interaction.eventId()).forEach(pair -> {
                    long candidate = pair.other(interaction.eventId());
                    if (!weights.containsKey(candidate)) {
                        candidates.merge(candidate, pair.score(), Math::max);
                    }
                }));
        return candidates.entrySet().stream()
                .map(entry -> new Recommendation(entry.getKey(), entry.getValue()))
                .sorted(ORDER).limit(limit)
                .map(candidate -> predict(candidate.eventId(), weights)).sorted(ORDER).toList();
    }

    private Recommendation predict(long eventId, Map<Long, Double> weights) {
        var nearest = store.similar(eventId).stream()
                .filter(pair -> weights.containsKey(pair.other(eventId))).limit(neighbors).toList();
        double denominator = nearest.stream().mapToDouble(RecommendationStore.Similarity::score).sum();
        double numerator = nearest.stream().mapToDouble(pair -> pair.score() * weights.get(pair.other(eventId))).sum();
        return new Recommendation(eventId, denominator == 0 ? 0 : numerator / denominator);
    }

    public List<Recommendation> counts(List<Long> eventIds) {
        return eventIds.stream().distinct().map(id -> new Recommendation(id, store.interactions(id))).toList();
    }
}

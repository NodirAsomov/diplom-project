package ru.practicum.ewm.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.client.RequestClient;
import ru.practicum.ewm.client.ReadResilience;
import ru.practicum.ewm.stats.client.AnalyzerClient;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventUtil {
    private final RequestClient requests;
    private final AnalyzerClient analyzer;
    private final ReadResilience resilience;

    public Map<Long, Long> confirmed(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return resilience.read("requests", () -> requests.counts(new ArrayList<>(eventIds)), Map::of);
    }

    public Map<Long, Double> ratings(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return analyzer.getInteractionsCount(eventIds).collect(Collectors.toMap(
                RecommendedEventProto::getEventId, RecommendedEventProto::getScore));
    }

    public Long getConfirmedRequests(Long id) {
        return confirmed(List.of(id)).getOrDefault(id, 0L);
    }

    public double getRating(Long id) {
        return ratings(List.of(id)).getOrDefault(id, 0.0);
    }
}

package ru.practicum.ewm.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@RequiredArgsConstructor
public class FeatureReader {
    private final FeatureClient client;
    private final ReadResilience resilience;

    public FeatureSummary summary(Collection<Long> ids, Long userId) {
        if (ids.isEmpty()) return new FeatureSummary(Map.of(), Map.of(), Set.of());
        return resilience.read("features", () -> client.summary(new ArrayList<>(ids), userId),
                () -> new FeatureSummary(Map.of(), Map.of(), Set.of()));
    }
}

package ru.practicum.ewm.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.client.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventUtil {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RequestClient requests;
    private final StatsFeignClient stats;
    private final ReadResilience resilience;

    public Map<Long, Long> confirmed(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();
        return resilience.read("requests", () -> requests.counts(new ArrayList<>(eventIds)), Map::of);
    }

    public Map<Long, Long> views(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();
        var uris = eventIds.stream().map(id -> "/events/" + id).toList();
        return resilience.read("statistics", () -> stats.stats("2020-01-01 00:00:00",
                LocalDateTime.now().format(FORMAT), uris, true).stream()
                .collect(Collectors.toMap(hit -> Long.valueOf(hit.getUri().substring("/events/".length())),
                        hit -> hit.getHits(), Long::sum)), Map::of);
    }

    public Long getConfirmedRequests(Long id) {
        return confirmed(List.of(id)).getOrDefault(id, 0L);
    }

    public Long getViews(Long id) {
        return views(List.of(id)).getOrDefault(id, 0L);
    }
}

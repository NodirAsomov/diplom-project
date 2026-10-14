package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {
    @GetMapping("/internal/requests/users/{userId}/events/{eventId}/confirmed")
    boolean hasConfirmed(@PathVariable("userId") long userId, @PathVariable("eventId") long eventId);

    @PostMapping("/internal/requests/counts")
    Map<Long, Long> counts(@RequestBody List<Long> eventIds);
}

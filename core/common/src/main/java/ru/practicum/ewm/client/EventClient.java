package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/internal/events/{id}")
    EventInfo get(@PathVariable("id") Long id);
}

package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "event-service", contextId = "EventCleanupClient")
public interface EventCleanupClient {
    @DeleteMapping("/internal/events/users/{id}")
    void deleteUser(@PathVariable("id") Long id);

}

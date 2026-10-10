package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "request-service", contextId = "RequestCleanupClient")
public interface RequestCleanupClient {
    @DeleteMapping("/internal/requests/users/{id}")
    void deleteUser(@PathVariable("id") Long id);

    @PostMapping("/internal/requests/delete-events")
    void deleteEvents(@RequestBody List<Long> ids);

}

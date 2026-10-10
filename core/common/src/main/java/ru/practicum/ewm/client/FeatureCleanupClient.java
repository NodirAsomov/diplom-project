package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "feature-service", contextId = "FeatureCleanupClient")
public interface FeatureCleanupClient {
    @DeleteMapping("/internal/features/users/{id}")
    void deleteUser(@PathVariable("id") Long id);

    @PostMapping("/internal/features/delete-events")
    void deleteEvents(@RequestBody List<Long> ids);

}

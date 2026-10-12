package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "feature-service")
public interface FeatureClient {
    @PostMapping("/internal/features/summary")
    FeatureSummary summary(@RequestBody List<Long> ids,
                           @RequestParam(value = "userId", required = false) Long userId);
}

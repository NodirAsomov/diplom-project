package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {
    @PostMapping("/internal/requests/counts")
    Map<Long, Long> counts(@RequestBody List<Long> eventIds);
}

package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.stats.dto.*;
import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsFeignClient {
    @GetMapping("/stats")
    List<StatHitResponseElement> stats(@RequestParam("start") String start,
                                     @RequestParam("end") String end,
                                     @RequestParam("uris") List<String> uris,
                                     @RequestParam("unique") boolean unique);

    @PostMapping("/hit")
    void hit(@RequestBody StatHitRequest hit);
}

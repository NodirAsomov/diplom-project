package ru.practicum.ewm.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class RequestInternalController {
    private final ParticipationRequestRepository repository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @PostMapping("/counts")
    @Transactional(readOnly = true)
    public Map<Long, Long> counts(@RequestBody List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();
        return repository.countConfirmed(eventIds).stream()
                .collect(Collectors.toMap(row -> row.getEventId(), row -> row.getCount()));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public void deleteUser(@PathVariable Long id) {
        jdbc.update("delete from participation_requests where requester_id = ?", id);
    }

    @PostMapping("/delete-events")
    @Transactional
    public void deleteEvents(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            jdbc.update("delete from participation_requests where event_id = ?", id);
        }
    }
}

package ru.practicum.ewm.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.EventInfo;
import ru.practicum.ewm.event.exception.EventNotFoundException;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.user.dto.UserShortDto;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class EventInternalController {
    private final EventRepository repository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final ru.practicum.ewm.client.RequestCleanupClient requests;
    private final ru.practicum.ewm.client.FeatureCleanupClient features;

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public EventInfo get(@PathVariable Long id) {
        var event = repository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
        return new EventInfo(event.getId(), new UserShortDto(event.getInitiatorId(), event.getInitiatorName()),
                event.getState(), event.getParticipantLimit(), event.getRequestModeration());
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public void deleteUser(@PathVariable Long id) {
        var ids = jdbc.queryForList("select event_id from events where initiator_id = ?", Long.class, id);
        if (!ids.isEmpty()) {
            requests.deleteEvents(ids);
            features.deleteEvents(ids);
            jdbc.update("delete from events where initiator_id = ?", id);
        }
    }
}

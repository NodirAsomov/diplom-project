package ru.practicum.ewm.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.client.StatsFeignClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class EventAdminController {

    private final EventService eventService;
    private final StatsFeignClient stats;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    public List<EventFullDto> getByAdmin(@RequestParam(required = false) List<Long> users,
                                         @RequestParam(required = false) List<String> states,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) String rangeStart,
                                         @RequestParam(required = false) String rangeEnd,
                                         @RequestParam(defaultValue = "0") Integer from,
                                         @RequestParam(defaultValue = "10") Integer size) {
        List<EventFullDto> result = eventService.getByAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
        if (!result.isEmpty()) {
            List<String> uris = result.stream().map(dto -> "/events/" + dto.getId()).toList();
            Map<String, Long> views = stats.stats("1970-01-01 00:00:00",
                            LocalDateTime.now().plusSeconds(1).format(FORMATTER), uris, true)
                    .stream().collect(Collectors.toMap(hit -> hit.getUri(), hit -> hit.getHits(), Long::sum));
            result.forEach(dto -> dto.setViews(views.getOrDefault("/events/" + dto.getId(), 0L)));
        }
        return result;
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateByAdmin(@PathVariable Long eventId,
                                      @Valid @RequestBody UpdateEventAdminRequest dto) {
        return eventService.updateByAdmin(eventId, dto);
    }
}

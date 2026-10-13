package ru.practicum.ewm.event.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.event.service.RecommendationFacade;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class EventPublicController {
    private final EventService eventService;
    private final CollectorClient collector;
    private final RecommendationFacade recommendations;

    @GetMapping("/events")
    public List<EventShortDto> getPublished(@RequestParam(required = false) String text,
                                           @RequestParam(required = false) List<Long> categories,
                                           @RequestParam(required = false) Boolean paid,
                                           @RequestParam(required = false) String rangeStart,
                                           @RequestParam(required = false) String rangeEnd,
                                           @RequestParam(required = false) Boolean onlyAvailable,
                                           @RequestParam(required = false) String sort,
                                           @RequestParam(defaultValue = "0") @Min(0) Integer from,
                                           @RequestParam(defaultValue = "10") @Min(1) Integer size,
                                           @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId) {
        return eventService.getPublished(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size, userId);
    }

    @GetMapping("/events/{id}")
    public EventFullDto getPublishedById(@PathVariable Long id,
                                        @RequestHeader("X-EWM-USER-ID") @Min(1) long userId) {
        EventFullDto result = eventService.getPublishedById(id);
        collector.collect(userId, id, ActionTypeProto.ACTION_VIEW);
        return result;
    }

    @GetMapping("/events/recommendations")
    public List<EventShortDto> recommendations(@RequestHeader("X-EWM-USER-ID") @Min(1) long userId,
                                               @RequestParam(defaultValue = "10") @Min(1) int size) {
        return recommendations.recommend(userId, size);
    }

    @PutMapping("/events/{eventId}/like")
    public void like(@PathVariable Long eventId, @RequestHeader("X-EWM-USER-ID") @Min(1) long userId) {
        recommendations.like(userId, eventId);
    }
}

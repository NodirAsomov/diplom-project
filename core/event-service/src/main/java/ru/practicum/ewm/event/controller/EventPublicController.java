package ru.practicum.ewm.event.controller;

import jakarta.validation.constraints.Min;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.event.service.RecommendationFacade;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.client.StatsFeignClient;
import ru.practicum.ewm.stats.dto.StatHitRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class EventPublicController {
    private final EventService eventService;
    private final CollectorClient collector;
    private final RecommendationFacade recommendations;
    private final StatsFeignClient stats;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
                                           @RequestHeader(value = "X-EWM-USER-ID", required = false) @Min(1) Long userId,
                                           HttpServletRequest request) {
        List<EventShortDto> result = eventService.getPublished(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size, userId);
        if (userId == null) {
            recordHit(request);
            Map<String, Long> views = views(result.stream().map(dto -> "/events/" + dto.getId()).toList());
            result.forEach(dto -> {
                dto.setViews(views.getOrDefault("/events/" + dto.getId(), 0L));
                dto.setRating(legacyRating(dto.getLikes(), dto.getDislikes()));
            });
        }
        return result;
    }

    @GetMapping("/events/{id}")
    public EventFullDto getPublishedById(@PathVariable Long id,
                                        @RequestHeader(value = "X-EWM-USER-ID", required = false) @Min(1) Long userId,
                                        HttpServletRequest request) {
        EventFullDto result = eventService.getPublishedById(id);
        if (userId == null) {
            recordHit(request);
            String uri = request.getRequestURI();
            result.setViews(views(List.of(uri)).getOrDefault(uri, 0L));
            result.setRating(legacyRating(result.getLikes(), result.getDislikes()));
        } else {
            collector.collect(userId, id, ActionTypeProto.ACTION_VIEW);
        }
        return result;
    }

    private void recordHit(HttpServletRequest request) {
        stats.hit(new StatHitRequest("ewm-main-service", request.getRequestURI(), request.getRemoteAddr(),
                LocalDateTime.now()));
    }

    private Map<String, Long> views(List<String> uris) {
        if (uris.isEmpty()) {
            return Map.of();
        }
        return stats.stats("1970-01-01 00:00:00", LocalDateTime.now().plusSeconds(1).format(FORMATTER), uris, true)
                .stream().collect(Collectors.toMap(hit -> hit.getUri(), hit -> hit.getHits(), Long::sum));
    }

    private double legacyRating(Long likes, Long dislikes) {
        return (likes == null ? 0L : likes) - (dislikes == null ? 0L : dislikes);
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

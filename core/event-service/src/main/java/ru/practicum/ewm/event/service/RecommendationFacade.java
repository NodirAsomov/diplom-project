package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.client.FeatureReader;
import ru.practicum.ewm.client.RequestClient;
import ru.practicum.ewm.common.exception.ValidationException;
import ru.practicum.ewm.common.util.EventUtil;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.exception.EventNotFoundException;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.rating.dto.RatingStatsDto;
import ru.practicum.ewm.stats.client.AnalyzerClient;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationFacade {
    private final AnalyzerClient analyzer;
    private final CollectorClient collector;
    private final EventRepository events;
    private final RequestClient requests;
    private final FeatureReader features;
    private final EventUtil util;

    public List<EventShortDto> recommend(long userId, int size) {
        var predictions = analyzer.getRecommendationsForUser(userId, size).toList();
        var ids = predictions.stream().map(item -> item.getEventId()).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        var found = events.findAllById(ids).stream().filter(event -> event.getState() == EventState.PUBLISHED)
                .collect(Collectors.toMap(Event::getId, Function.identity()));
        var ratings = util.ratings(ids);
        var confirmed = util.confirmed(ids);
        var summary = features.summary(ids, userId);
        return predictions.stream().map(item -> found.get(item.getEventId())).filter(Objects::nonNull)
                .map(event -> EventMapper.toEventShortDto(event, ratings.getOrDefault(event.getId(), 0.0),
                        confirmed.getOrDefault(event.getId(), 0L),
                        summary.getRatings().getOrDefault(event.getId(), RatingStatsDto.EMPTY),
                        summary.getComments().getOrDefault(event.getId(), 0L))).toList();
    }

    public void like(long userId, long eventId) {
        Event event = events.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        if (!event.getEventDate().isBefore(LocalDateTime.now()) || !requests.hasConfirmed(userId, eventId)) {
            throw new ValidationException("Only attended events can be liked");
        }
        collector.collect(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }
}

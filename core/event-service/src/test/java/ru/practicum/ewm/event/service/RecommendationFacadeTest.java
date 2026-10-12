package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.client.FeatureReader;
import ru.practicum.ewm.client.RequestClient;
import ru.practicum.ewm.common.exception.ValidationException;
import ru.practicum.ewm.common.util.EventUtil;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.stats.client.AnalyzerClient;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationFacadeTest {
    private final EventRepository events = mock(EventRepository.class);
    private final RequestClient requests = mock(RequestClient.class);
    private final CollectorClient collector = mock(CollectorClient.class);
    private final RecommendationFacade facade = new RecommendationFacade(mock(AnalyzerClient.class), collector,
            events, requests, mock(FeatureReader.class), mock(EventUtil.class));

    @Test
    void allowsLikeOnlyAfterEventAndForConfirmedParticipant() {
        var event = Event.builder().id(4L).state(EventState.PUBLISHED)
                .eventDate(LocalDateTime.now().minusDays(1)).build();
        when(events.findByIdAndState(4L, EventState.PUBLISHED)).thenReturn(Optional.of(event));
        when(requests.hasConfirmed(7, 4)).thenReturn(true);
        facade.like(7, 4);
        verify(collector).collect(7, 4, ActionTypeProto.ACTION_LIKE);
    }

    @Test
    void rejectsLikeByNonParticipantAndBeforeEventWithoutSendingAction() {
        var event = Event.builder().id(4L).state(EventState.PUBLISHED)
                .eventDate(LocalDateTime.now().minusDays(1)).build();
        when(events.findByIdAndState(4L, EventState.PUBLISHED)).thenReturn(Optional.of(event));
        assertThrows(ValidationException.class, () -> facade.like(7, 4));
        event.setEventDate(LocalDateTime.now().plusDays(1));
        when(requests.hasConfirmed(7, 4)).thenReturn(true);
        assertThrows(ValidationException.class, () -> facade.like(7, 4));
        verifyNoInteractions(collector);
    }
}

package ru.practicum.ewm.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import ru.practicum.ewm.client.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventUtilTest {
    private final RequestClient requests = mock(RequestClient.class);
    private final ru.practicum.ewm.stats.client.AnalyzerClient stats = mock(ru.practicum.ewm.stats.client.AnalyzerClient.class);

    private EventUtil util() {
        CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
        when(factory.create(anyString())).thenReturn(new CircuitBreaker() {
            @Override
            public <T> T run(Supplier<T> operation, Function<Throwable, T> fallback) {
                try {
                    return operation.get();
                } catch (RuntimeException error) {
                    return fallback.apply(error);
                }
            }
        });
        return new EventUtil(requests, stats, new ReadResilience(factory));
    }

    @Test
    void loadsCountersInOneRequestForTheWholePage() {
        var ids = List.of(1L, 2L, 3L);
        when(requests.counts(ids)).thenReturn(Map.of(1L, 4L));
        assertEquals(Map.of(1L, 4L), util().confirmed(ids));
        verify(requests, times(1)).counts(ids);
    }

    @Test
    void unavailableRequestsReturnZeroAfterOneRetry() {
        when(requests.counts(List.of(1L))).thenThrow(new IllegalStateException("offline"));
        assertEquals(0L, util().getConfirmedRequests(1L));
        verify(requests, times(2)).counts(List.of(1L));
    }

    @Test
    void ratingIsTheInteractionScoreReturnedByAnalyzer() {
        var ids = List.of(1L, 2L);
        when(stats.getInteractionsCount(ids)).thenReturn(java.util.stream.Stream.of(
                ru.practicum.ewm.stats.proto.RecommendedEventProto.newBuilder()
                        .setEventId(1L).setScore(1.8).build(),
                ru.practicum.ewm.stats.proto.RecommendedEventProto.newBuilder()
                        .setEventId(2L).setScore(0.4).build()));
        assertEquals(Map.of(1L, 1.8, 2L, 0.4), util().ratings(ids));
        verify(stats).getInteractionsCount(ids);
        verifyNoInteractions(requests);
    }

    @Test
    void emptyPageNeverCallsOtherServices() {
        var util = util();
        assertTrue(util.confirmed(List.of()).isEmpty());
        assertTrue(util.ratings(List.of()).isEmpty());
        verifyNoInteractions(requests, stats);
    }
}

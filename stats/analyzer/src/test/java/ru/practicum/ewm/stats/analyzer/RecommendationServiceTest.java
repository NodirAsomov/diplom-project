package ru.practicum.ewm.stats.analyzer;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {
    private final RecommendationStore store = mock(RecommendationStore.class);
    private final RecommendationService service = new RecommendationService(store, 2);

    @Test
    void excludesSeenEventsAndSortsBothPairOrientations() {
        when(store.history(7)).thenReturn(List.of(new RecommendationStore.Interaction(3, 0.4, Instant.EPOCH)));
        when(store.similar(2)).thenReturn(List.of(
                new RecommendationStore.Similarity(2, 3, 0.9),
                new RecommendationStore.Similarity(1, 2, 0.7),
                new RecommendationStore.Similarity(2, 4, 0.8)));
        assertEquals(List.of(new RecommendationService.Recommendation(4, 0.8),
                new RecommendationService.Recommendation(1, 0.7)), service.similar(2, 7, 2));
    }

    @Test
    void deduplicatesCandidatesAndPredictsFromNearestViewedEvents() {
        when(store.history(7)).thenReturn(List.of(
                new RecommendationStore.Interaction(1, 1, Instant.ofEpochSecond(3)),
                new RecommendationStore.Interaction(2, 0.4, Instant.ofEpochSecond(2)),
                new RecommendationStore.Interaction(3, 0.8, Instant.ofEpochSecond(1))));
        when(store.similar(1)).thenReturn(List.of(new RecommendationStore.Similarity(1, 4, 0.8)));
        when(store.similar(2)).thenReturn(List.of(new RecommendationStore.Similarity(2, 4, 0.7)));
        when(store.similar(4)).thenReturn(List.of(
                new RecommendationStore.Similarity(4, 9, 0.99),
                new RecommendationStore.Similarity(1, 4, 0.8),
                new RecommendationStore.Similarity(2, 4, 0.7),
                new RecommendationStore.Similarity(3, 4, 0.1)));
        var result = service.recommend(7, 2);
        assertEquals(1, result.size());
        assertEquals(4, result.getFirst().eventId());
        assertEquals((0.8 + 0.7 * 0.4) / 1.5, result.getFirst().score(), 1e-12);
        verify(store, never()).similar(3);
    }

    @Test
    void emptyHistoryAndZeroLimitProduceNoRecommendations() {
        when(store.history(7)).thenReturn(List.of());
        assertTrue(service.recommend(7, 2).isEmpty());
        assertTrue(service.recommend(7, 0).isEmpty());
        assertTrue(service.similar(2, 7, 0).isEmpty());
    }

    @Test
    void countsReturnZeroForUnknownEventsAndDeduplicateIds() {
        when(store.interactions(2)).thenReturn(1.8);
        assertEquals(List.of(new RecommendationService.Recommendation(2, 1.8),
                new RecommendationService.Recommendation(3, 0)), service.counts(List.of(2L, 3L, 2L)));
        assertTrue(service.counts(List.of()).isEmpty());
    }
}

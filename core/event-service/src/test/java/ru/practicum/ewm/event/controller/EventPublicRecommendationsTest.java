package ru.practicum.ewm.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.exception.EventNotFoundException;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.event.service.RecommendationFacade;
import ru.practicum.ewm.stats.client.CollectorClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.client.StatsFeignClient;
import ru.practicum.ewm.stats.dto.StatHitResponseElement;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventPublicController.class)
class EventPublicRecommendationsTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private EventService events;

    @MockBean
    private CollectorClient collector;

    @MockBean
    private RecommendationFacade recommendations;

    @MockBean
    private StatsFeignClient stats;

    @Test
    void detailReturnsAnalyzerRatingAndRecordsViewForIdentifiedUser() throws Exception {
        when(events.getPublishedById(4L)).thenReturn(EventFullDto.builder().id(4L).rating(1.8).build());
        mvc.perform(get("/events/4").header("X-EWM-USER-ID", 7))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(1.8))
                .andExpect(jsonPath("$.views").doesNotExist());
        verify(collector).collect(7, 4, ActionTypeProto.ACTION_VIEW);
        verifyNoInteractions(stats);
    }

    @Test
    void listReturnsRatingWithoutRecordingViews() throws Exception {
        when(events.getPublished(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(EventShortDto.builder().id(4L).rating(0.8).build()));
        mvc.perform(get("/events").header("X-EWM-USER-ID", 7)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(0.8))
                .andExpect(jsonPath("$[0].views").doesNotExist());
        verifyNoInteractions(collector, stats);
    }

    @Test
    void invalidUserHeaderIsRejectedBeforeLoadingEventOrRecordingView() throws Exception {
        mvc.perform(get("/events/4").header("X-EWM-USER-ID", 0)).andExpect(status().isBadRequest());
        mvc.perform(get("/events/4").header("X-EWM-USER-ID", "invalid")).andExpect(status().isBadRequest());
        verifyNoInteractions(events, collector, stats);
    }

    @Test
    void unpublishedEventReturnsNotFoundWithoutRecordingView() throws Exception {
        when(events.getPublishedById(4L)).thenThrow(new EventNotFoundException(4L));
        mvc.perform(get("/events/4").header("X-EWM-USER-ID", 7)).andExpect(status().isNotFound());
        mvc.perform(get("/events/4")).andExpect(status().isNotFound());
        verifyNoInteractions(collector, stats);
    }

    @Test
    void anonymousDetailReturnsUniqueViewsAndLegacyRatingAfterLikeDislikeAndReplacement() throws Exception {
        when(stats.stats(anyString(), anyString(), eq(List.of("/events/4")), eq(true)))
                .thenReturn(List.of(new StatHitResponseElement("ewm-main-service", "/events/4", 1L)));
        long[][] votes = {{1L, 0L}, {0L, 1L}, {0L, 1L}};
        for (long[] vote : votes) {
            when(events.getPublishedById(4L)).thenReturn(EventFullDto.builder().id(4L)
                    .rating(1.8).likes(vote[0]).dislikes(vote[1]).build());
            mvc.perform(get("/events/4")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.views").value(1))
                    .andExpect(jsonPath("$.rating").value((double) (vote[0] - vote[1])));
        }
        verify(stats, times(3)).hit(argThat(hit -> hit.getAppName().equals("ewm-main-service")
                && hit.getUri().equals("/events/4") && hit.getIp().equals("127.0.0.1")
                && hit.getTimestamp() != null));
        verifyNoInteractions(collector);
    }

    @Test
    void anonymousListIncludesViewsAndRecordsEachRequestEvenWhenEmpty() throws Exception {
        when(events.getPublished(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(EventShortDto.builder().id(4L).likes(1L).dislikes(0L).rating(1.8).build()),
                        List.of());
        when(stats.stats(anyString(), anyString(), eq(List.of("/events/4")), eq(true)))
                .thenReturn(List.of(new StatHitResponseElement("ewm-main-service", "/events/4", 2L)));
        mvc.perform(get("/events").param("text", "concert")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].views").value(2))
                .andExpect(jsonPath("$[0].rating").value(1.0));
        mvc.perform(get("/events")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(stats, times(2)).hit(argThat(hit -> hit.getUri().equals("/events")));
        verify(stats).stats(anyString(), anyString(), eq(List.of("/events/4")), eq(true));
        verifyNoMoreInteractions(stats);
        verifyNoInteractions(collector);
    }

    @Test
    void recommendationsRouteUsesUserIdAndLikeDelegatesValidation() throws Exception {
        when(recommendations.recommend(7, 3)).thenReturn(List.of());
        mvc.perform(get("/events/recommendations").header("X-EWM-USER-ID", 7).param("size", "3"))
                .andExpect(status().isOk());
        verify(recommendations).recommend(7, 3);
        mvc.perform(put("/events/4/like").header("X-EWM-USER-ID", 7)).andExpect(status().isOk());
        verify(recommendations).like(7, 4);
    }
}

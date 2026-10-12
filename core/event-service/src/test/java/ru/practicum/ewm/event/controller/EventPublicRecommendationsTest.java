package ru.practicum.ewm.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.event.dto.EventFullDto;
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
    void anonymousDetailCountsViewsAndIdentifiedDetailAlsoSendsRecommendationAction() throws Exception {
        when(events.getPublished(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        mvc.perform(get("/events")).andExpect(status().isOk());
        verifyNoInteractions(collector);
        when(events.getPublishedById(4L)).thenReturn(EventFullDto.builder().id(4L).rating(1.8).build());
        when(stats.stats(anyString(), anyString(), eq(List.of("/events/4")), eq(true)))
                .thenReturn(List.of(new StatHitResponseElement("ewm-main-service", "/events/4", 1L)));
        mvc.perform(get("/events/4"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.views").value(1));
        verify(stats).hit(argThat(hit -> hit.getUri().equals("/events/4")
                && hit.getIp().equals("127.0.0.1")));
        verifyNoInteractions(collector);
        mvc.perform(get("/events/4").header("X-EWM-USER-ID", 7))
                .andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(1.8))
                .andExpect(jsonPath("$.views").value(1));
        verify(collector).collect(7, 4, ActionTypeProto.ACTION_VIEW);
    }

    @Test
    void unpublishedEventReturnsNotFoundWithoutRecordingView() throws Exception {
        when(events.getPublishedById(4L)).thenThrow(new EventNotFoundException(4L));
        mvc.perform(get("/events/4")).andExpect(status().isNotFound());
        verifyNoInteractions(stats, collector);
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

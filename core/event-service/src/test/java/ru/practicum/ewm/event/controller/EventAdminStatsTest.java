package ru.practicum.ewm.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.client.StatsFeignClient;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.service.EventService;
import ru.practicum.ewm.stats.dto.StatHitResponseElement;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventAdminController.class)
class EventAdminStatsTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private EventService events;

    @MockBean
    private StatsFeignClient stats;

    @Test
    void searchReturnsUniqueViewsAndZeroForEventsWithoutVisits() throws Exception {
        when(events.getByAdmin(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(
                EventFullDto.builder().id(4L).rating(1.8).build(),
                EventFullDto.builder().id(5L).rating(0.4).build()));
        when(stats.stats(anyString(), anyString(), eq(List.of("/events/4", "/events/5")), eq(true)))
                .thenReturn(List.of(new StatHitResponseElement("ewm-main-service", "/events/4", 3L)));
        mvc.perform(get("/admin/events").param("users", "7").param("states", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].views").value(3))
                .andExpect(jsonPath("$[1].views").value(0))
                .andExpect(jsonPath("$[0].rating").value(1.8));
        verify(stats).stats(anyString(), anyString(), eq(List.of("/events/4", "/events/5")), eq(true));
        verifyNoMoreInteractions(stats);
    }

    @Test
    void emptySearchDoesNotQueryOrRecordStatistics() throws Exception {
        when(events.getByAdmin(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        mvc.perform(get("/admin/events")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verifyNoInteractions(stats);
    }
}

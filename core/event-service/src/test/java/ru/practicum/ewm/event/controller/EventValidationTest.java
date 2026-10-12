package ru.practicum.ewm.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.event.service.EventService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventPrivateController.class)
class EventValidationTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private EventService events;

    @Test
    void missingJsonBodyReturnsBadRequest() throws Exception {
        mvc.perform(post("/users/1/events").contentType("application/json"))
                .andExpect(status().isBadRequest());
    }
}
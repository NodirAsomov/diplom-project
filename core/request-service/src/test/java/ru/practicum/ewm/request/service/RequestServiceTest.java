package ru.practicum.ewm.request.service;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.client.*;
import ru.practicum.ewm.event.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.request.exception.RequestNotFoundException;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.*;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.dto.UserShortDto;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestServiceTest {
    @Test
    void cannotConfirmRequestFromAnotherEvent() {
        var repository = mock(ParticipationRequestRepository.class);
        var users = mock(UserLookup.class);
        var events = mock(EventLookup.class);
        var locks = mock(RequestLock.class);
        when(events.findById(1L)).thenReturn(Optional.of(
                new EventInfo(1L, new UserShortDto(5L, "owner"), EventState.PUBLISHED, 10, true)));
        var foreign = ParticipationRequest.builder().id(7L).eventId(2L)
                .requesterId(9L).status(RequestStatus.PENDING).build();
        when(repository.findByIdIn(List.of(7L))).thenReturn(List.of(foreign));
        var service = new RequestServiceImpl(repository, users, events, new RequestMapper(), locks);
        var input = new EventRequestStatusUpdateRequest();
        input.setRequestIds(List.of(7L));
        input.setStatus("CONFIRMED");
        assertThrows(RequestNotFoundException.class, () -> service.changeStatus(5L, 1L, input));
        verify(repository, never()).saveAll(any());
    }
}

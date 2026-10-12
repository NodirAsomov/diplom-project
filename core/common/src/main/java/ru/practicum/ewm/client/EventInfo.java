package ru.practicum.ewm.client;

import lombok.*;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.user.dto.UserShortDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventInfo {
    private Long id;
    private UserShortDto initiator;
    private EventState state;
    private Integer participantLimit;
    private Boolean requestModeration;
}

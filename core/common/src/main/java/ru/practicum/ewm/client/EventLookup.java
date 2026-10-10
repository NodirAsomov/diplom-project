package ru.practicum.ewm.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.event.model.EventState;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventLookup {
    private final EventClient client;

    public Optional<EventInfo> findById(Long id) {
        try {
            return Optional.of(client.get(id));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }

    public Optional<EventInfo> findByIdAndState(Long id, EventState state) {
        return findById(id).filter(event -> event.getState() == state);
    }
}

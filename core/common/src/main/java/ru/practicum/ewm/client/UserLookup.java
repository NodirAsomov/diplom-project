package ru.practicum.ewm.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.user.dto.UserDto;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserLookup {
    private final UserClient client;

    public Optional<UserDto> findById(Long id) {
        try {
            return Optional.of(client.get(id));
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
    }
}

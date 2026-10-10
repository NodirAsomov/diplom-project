package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.user.dto.UserDto;
import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/internal/users/{id}")
    UserDto get(@PathVariable("id") Long id);

    @PostMapping("/internal/users/batch")
    List<UserDto> getBatch(@RequestBody List<Long> ids);
}

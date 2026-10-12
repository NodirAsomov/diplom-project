package ru.practicum.ewm.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.exception.UserNotFoundException;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.repository.UserRepository;
import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {
    private final UserRepository repository;
    private final UserMapper mapper;

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return mapper.toUserDto(repository.findById(id).orElseThrow(() -> new UserNotFoundException(id)));
    }

    @PostMapping("/batch")
    public List<UserDto> batch(@RequestBody List<Long> ids) {
        return repository.findAllById(ids).stream().map(mapper::toUserDto).toList();
    }
}

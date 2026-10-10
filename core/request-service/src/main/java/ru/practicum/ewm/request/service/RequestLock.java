package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestLock {
    private final JdbcTemplate jdbc;

    public void lock(Long eventId) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement("select pg_advisory_xact_lock(?)")) {
                statement.setLong(1, eventId);
                statement.execute();
            }
            return null;
        });
    }
}

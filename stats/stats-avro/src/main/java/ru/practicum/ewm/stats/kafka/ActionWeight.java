package ru.practicum.ewm.stats.kafka;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;

public final class ActionWeight {
    private ActionWeight() {
    }

    public static double of(ActionTypeAvro action) {
        return switch (action) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}

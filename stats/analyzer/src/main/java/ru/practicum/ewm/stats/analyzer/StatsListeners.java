package ru.practicum.ewm.stats.analyzer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class StatsListeners {
    private final RecommendationStore store;

    public StatsListeners(RecommendationStore store) {
        this.store = store;
    }

    @KafkaListener(topics = "${stats.topics.actions}", groupId = "analyzer-actions",
            properties = "value.deserializer=ru.practicum.ewm.stats.kafka.UserActionDeserializer")
    public void action(UserActionAvro action) {
        store.save(action);
    }

    @KafkaListener(topics = "${stats.topics.similarities}", groupId = "analyzer-similarities",
            properties = "value.deserializer=ru.practicum.ewm.stats.kafka.EventSimilarityDeserializer")
    public void similarity(EventSimilarityAvro similarity) {
        store.save(similarity);
    }
}

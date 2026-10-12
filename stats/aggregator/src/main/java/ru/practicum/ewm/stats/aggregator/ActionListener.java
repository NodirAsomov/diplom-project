package ru.practicum.ewm.stats.aggregator;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ActionListener implements ConsumerSeekAware {
    private final SimilarityCalculator calculator;
    private final KafkaTemplate<String, Object> kafka;
    private final String topic;

    public ActionListener(SimilarityCalculator calculator, KafkaTemplate<String, Object> kafka,
                          @Value("${stats.topics.similarities}") String topic) {
        this.calculator = calculator;
        this.kafka = kafka;
        this.topic = topic;
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        calculator.reset();
        callback.seekToBeginning(assignments.keySet());
    }

    @KafkaListener(topics = "${stats.topics.actions}", groupId = "aggregator")
    public void onAction(UserActionAvro action) {
        calculator.accept(action, similarity -> {
            try {
                kafka.send(topic, similarity.getEventA() + ":" + similarity.getEventB(), similarity)
                        .get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while publishing similarity", e);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot publish similarity", e);
            }
        });
    }
}

package ru.practicum.ewm.stats.kafka;

import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import java.io.IOException;

public class EventSimilarityDeserializer implements Deserializer<EventSimilarityAvro> {
    @Override
    public EventSimilarityAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return new SpecificDatumReader<>(EventSimilarityAvro.class)
                    .read(null, DecoderFactory.get().binaryDecoder(data, null));
        } catch (IOException e) {
            throw new SerializationException("Cannot deserialize EventSimilarity", e);
        }
    }
}

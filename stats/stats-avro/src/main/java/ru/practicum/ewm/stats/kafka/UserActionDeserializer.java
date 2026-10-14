package ru.practicum.ewm.stats.kafka;

import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import java.io.IOException;

public class UserActionDeserializer implements Deserializer<UserActionAvro> {
    @Override
    public UserActionAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return new SpecificDatumReader<>(UserActionAvro.class)
                    .read(null, DecoderFactory.get().binaryDecoder(data, null));
        } catch (IOException e) {
            throw new SerializationException("Cannot deserialize UserAction", e);
        }
    }
}

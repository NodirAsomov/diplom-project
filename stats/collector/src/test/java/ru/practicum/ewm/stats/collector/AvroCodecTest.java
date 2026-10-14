package ru.practicum.ewm.stats.collector;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.*;
import ru.practicum.ewm.stats.kafka.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class AvroCodecTest {
    @Test
    void bothRecordTypesRoundTripWithMillisecondTimestamp() {
        var serializer = new AvroSerializer();
        var action = new UserActionAvro(3L, 4L, ActionTypeAvro.REGISTER, Instant.ofEpochMilli(12345));
        assertEquals(action, new UserActionDeserializer().deserialize("actions", serializer.serialize("actions", action)));
        var similarity = new EventSimilarityAvro(3L, 4L, 0.7, action.getTimestamp());
        assertEquals(similarity, new EventSimilarityDeserializer().deserialize(
                "similarities", serializer.serialize("similarities", similarity)));
        assertNull(serializer.serialize("actions", null));
        assertNull(new UserActionDeserializer().deserialize("actions", null));
        assertNull(new EventSimilarityDeserializer().deserialize("similarities", null));
    }
}

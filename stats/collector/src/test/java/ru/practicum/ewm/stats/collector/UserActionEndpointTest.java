package ru.practicum.ewm.stats.collector;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserActionEndpointTest {
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final StreamObserver<Empty> observer = mock(StreamObserver.class);
    private final UserActionEndpoint endpoint = new UserActionEndpoint(kafka, "actions");

    private UserActionProto request() {
        return UserActionProto.newBuilder().setUserId(1).setEventId(2)
                .setActionType(ActionTypeProto.ACTION_LIKE)
                .setTimestamp(Timestamp.newBuilder().setSeconds(5).setNanos(123000000)).build();
    }

    @Test
    void acknowledgesOnlySuccessfulKafkaWriteAndPreservesTimestamp() {
        when(kafka.send(eq("actions"), eq("1"), any())).thenReturn(CompletableFuture.completedFuture(null));
        endpoint.collectUserAction(request(), observer);
        verify(kafka).send(eq("actions"), eq("1"), argThat(value ->
                value instanceof UserActionAvro action && action.getTimestamp().toEpochMilli() == 5123));
        verify(observer).onNext(Empty.getDefaultInstance());
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());
    }

    @Test
    void kafkaFailureDoesNotAcknowledgeAction() {
        when(kafka.send(eq("actions"), eq("1"), any())).thenReturn(
                CompletableFuture.failedFuture(new IllegalStateException("offline")));
        endpoint.collectUserAction(request(), observer);
        verify(observer).onError(argThat(error -> Status.fromThrowable(error).getCode() == Status.Code.UNAVAILABLE));
        verify(observer, never()).onCompleted();
        verify(observer, never()).onNext(any());
    }

    @Test
    void rejectsUnknownActionAndMissingTimestamp() {
        endpoint.collectUserAction(request().toBuilder().setActionTypeValue(99).build(), observer);
        endpoint.collectUserAction(request().toBuilder().clearTimestamp().build(), observer);
        verify(observer, times(2)).onError(argThat(error ->
                Status.fromThrowable(error).getCode() == Status.Code.INVALID_ARGUMENT));
        verifyNoInteractions(kafka);
    }
}

package ru.practicum.ewm.stats.collector;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@GrpcService
public class UserActionEndpoint extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final KafkaTemplate<String, Object> kafka;
    private final String topic;

    public UserActionEndpoint(KafkaTemplate<String, Object> kafka,
                              @Value("${stats.topics.actions}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> observer) {
        if (request.getUserId() <= 0 || request.getEventId() <= 0 || !request.hasTimestamp()
                || request.getActionTypeValue() > 2 || request.getActionTypeValue() < 0
                || request.getTimestamp().getNanos() < 0 || request.getTimestamp().getNanos() > 999999999
                || request.getTimestamp().getSeconds() < -62135596800L
                || request.getTimestamp().getSeconds() > 253402300799L) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription("Invalid user action").asRuntimeException());
            return;
        }
        var type = switch (request.getActionType()) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action");
        };
        var timestamp = Instant.ofEpochSecond(request.getTimestamp().getSeconds(), request.getTimestamp().getNanos());
        var action = new UserActionAvro(request.getUserId(), request.getEventId(), type, timestamp);
        try {
            kafka.send(topic, Long.toString(action.getUserId()), action).get(5, TimeUnit.SECONDS);
            observer.onNext(Empty.getDefaultInstance());
            observer.onCompleted();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            observer.onError(Status.CANCELLED.withCause(e).asRuntimeException());
        } catch (Exception e) {
            observer.onError(Status.UNAVAILABLE.withDescription("Kafka did not acknowledge action")
                    .withCause(e).asRuntimeException());
        }
    }
}

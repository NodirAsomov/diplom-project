package ru.practicum.ewm.stats.client;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import ru.practicum.ewm.stats.proto.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "stats.smoke", matches = "true")
class RecommendationSmokeTest {
    @Test
    void actionsTravelThroughKafkaAndBecomeRecommendations() throws Exception {
        var collectorChannel = ManagedChannelBuilder.forAddress("localhost", 9095).usePlaintext().build();
        var analyzerChannel = ManagedChannelBuilder.forAddress("localhost", 9096).usePlaintext().build();
        try {
            var collector = UserActionControllerGrpc.newBlockingStub(collectorChannel).withDeadlineAfter(10, TimeUnit.SECONDS);
            var analyzer = RecommendationsControllerGrpc.newBlockingStub(analyzerChannel);
            long firstUser = System.currentTimeMillis();
            long secondUser = firstUser + 1;
            long eventA = firstUser + 10;
            long eventB = firstUser + 20;
            long eventC = firstUser + 30;
            collect(collector, firstUser, eventA, ActionTypeProto.ACTION_LIKE);
            collect(collector, firstUser, eventB, ActionTypeProto.ACTION_REGISTER);
            collect(collector, secondUser, eventA, ActionTypeProto.ACTION_LIKE);
            collect(collector, secondUser, eventC, ActionTypeProto.ACTION_LIKE);
            collect(collector, firstUser, eventA, ActionTypeProto.ACTION_VIEW);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(40);
            List<RecommendedEventProto> recommendations = List.of();
            while (System.nanoTime() < deadline) {
                recommendations = list(analyzer.withDeadlineAfter(5, TimeUnit.SECONDS).getRecommendationsForUser(
                        UserPredictionsRequestProto.newBuilder().setUserId(firstUser).setMaxResults(5).build()));
                if (recommendations.size() == 1 && recommendations.getFirst().getEventId() == eventC) {
                    break;
                }
                Thread.sleep(250);
            }
            assertEquals(1, recommendations.size());
            assertEquals(eventC, recommendations.getFirst().getEventId());
            assertEquals(1.0, recommendations.getFirst().getScore(), 1e-12);
            var counts = list(analyzer.withDeadlineAfter(5, TimeUnit.SECONDS).getInteractionsCount(
                    InteractionsCountRequestProto.newBuilder().addAllEventId(List.of(eventA, eventB, eventC)).build()));
            assertEquals(List.of(2.0, 0.8, 1.0), counts.stream().map(RecommendedEventProto::getScore).toList());
            var similar = list(analyzer.withDeadlineAfter(5, TimeUnit.SECONDS).getSimilarEvents(
                    SimilarEventsRequestProto.newBuilder().setEventId(eventA).setUserId(firstUser).setMaxResults(5).build()));
            assertEquals(1, similar.size());
            assertEquals(eventC, similar.getFirst().getEventId());
            assertEquals(1 / Math.sqrt(2), similar.getFirst().getScore(), 1e-12);
        } finally {
            collectorChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            analyzerChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void collect(UserActionControllerGrpc.UserActionControllerBlockingStub stub, long userId,
                         long eventId, ActionTypeProto action) {
        Instant now = Instant.now();
        stub.collectUserAction(UserActionProto.newBuilder().setUserId(userId).setEventId(eventId).setActionType(action)
                .setTimestamp(Timestamp.newBuilder().setSeconds(now.getEpochSecond()).setNanos(now.getNano())).build());
    }

    private List<RecommendedEventProto> list(java.util.Iterator<RecommendedEventProto> iterator) {
        List<RecommendedEventProto> result = new ArrayList<>();
        iterator.forEachRemaining(result::add);
        return result;
    }
}

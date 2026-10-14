package ru.practicum.ewm.stats.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
public class AnalyzerClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        return stream(stub().getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                .setUserId(userId).setMaxResults(maxResults).build()));
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        return stream(stub().getSimilarEvents(SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId).setUserId(userId).setMaxResults(maxResults).build()));
    }

    public Stream<RecommendedEventProto> getInteractionsCount(Collection<Long> eventIds) {
        return stream(stub().getInteractionsCount(InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds).build()));
    }

    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub stub() {
        return client.withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    private Stream<RecommendedEventProto> stream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}

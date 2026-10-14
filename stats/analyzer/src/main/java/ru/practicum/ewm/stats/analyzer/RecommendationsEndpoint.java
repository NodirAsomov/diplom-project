package ru.practicum.ewm.stats.analyzer;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.*;
import java.util.List;
import java.util.function.Supplier;

@GrpcService
public class RecommendationsEndpoint extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService service;

    public RecommendationsEndpoint(RecommendationService service) {
        this.service = service;
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> observer) {
        respond(request.getUserId() > 0 && request.getMaxResults() >= 0,
                () -> service.recommend(request.getUserId(), request.getMaxResults()), observer);
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> observer) {
        respond(request.getEventId() > 0 && request.getUserId() > 0 && request.getMaxResults() >= 0,
                () -> service.similar(request.getEventId(), request.getUserId(), request.getMaxResults()), observer);
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> observer) {
        respond(request.getEventIdList().stream().allMatch(id -> id > 0),
                () -> service.counts(request.getEventIdList()), observer);
    }

    private void respond(boolean valid, Supplier<List<RecommendationService.Recommendation>> query,
                         StreamObserver<RecommendedEventProto> observer) {
        if (!valid) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription("Invalid recommendation request").asRuntimeException());
            return;
        }
        try {
            for (var item : query.get()) {
                observer.onNext(RecommendedEventProto.newBuilder().setEventId(item.eventId()).setScore(item.score()).build());
            }
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(Status.INTERNAL.withDescription("Cannot query recommendations").withCause(e).asRuntimeException());
        }
    }
}

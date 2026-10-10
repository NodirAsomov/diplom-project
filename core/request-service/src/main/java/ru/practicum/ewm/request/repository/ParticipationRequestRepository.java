package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    List<ParticipationRequest> findByIdIn(List<Long> requestIds);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    @org.springframework.data.jpa.repository.Query("""
            select r.eventId as eventId, count(r) as count
            from ParticipationRequest r
            where r.eventId in :eventIds and r.status = ru.practicum.ewm.request.model.RequestStatus.CONFIRMED
            group by r.eventId
            """)
    List<ConfirmedCount> countConfirmed(@org.springframework.data.repository.query.Param("eventIds") List<Long> eventIds);

    interface ConfirmedCount {
        Long getEventId();

        Long getCount();
    }
}

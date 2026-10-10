package ru.practicum.ewm.feature;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.client.FeatureSummary;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.rating.service.RatingService;
import ru.practicum.ewm.subscription.model.SubscriptionStatus;
import ru.practicum.ewm.subscription.repository.SubscriptionRepository;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/features")
@RequiredArgsConstructor
public class FeatureInternalController {
    private final CommentRepository comments;
    private final RatingService ratings;
    private final SubscriptionRepository subscriptions;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @PostMapping("/summary")
    @Transactional(readOnly = true)
    public FeatureSummary summary(@RequestBody List<Long> ids,
                                  @RequestParam(required = false) Long userId) {
        var counts = ids.isEmpty() ? Collections.<Long, Long>emptyMap()
                : comments.countByEventIdIn(ids).stream()
                .collect(Collectors.toMap(row -> row.getEventId(), row -> row.getCnt()));
        var followed = userId == null ? Collections.<Long>emptySet()
                : subscriptions.findByFollowerIdAndStatus(userId, SubscriptionStatus.CONFIRMED).stream()
                .map(sub -> sub.getFollowedId()).collect(Collectors.toSet());
        return new FeatureSummary(ratings.getStats(ids), counts, followed);
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public void deleteUser(@PathVariable Long id) {
        jdbc.update("delete from comments where author_id = ?", id);
        jdbc.update("delete from event_ratings where user_id = ?", id);
        jdbc.update("delete from subscriptions where follower_id = ? or followed_id = ?", id, id);
    }

    @PostMapping("/delete-events")
    @Transactional
    public void deleteEvents(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            jdbc.update("delete from comments where event_id = ?", id);
            jdbc.update("delete from event_ratings where event_id = ?", id);
        }
    }
}

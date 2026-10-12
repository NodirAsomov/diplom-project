package ru.practicum.ewm.stats.analyzer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.kafka.ActionWeight;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class RecommendationStore {
    private final JdbcTemplate jdbc;

    public RecommendationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Interaction(long eventId, double weight, Instant timestamp) {
    }

    public record Similarity(long eventA, long eventB, double score) {
        public long other(long event) {
            return eventA == event ? eventB : eventA;
        }
    }

    @Transactional
    public void save(UserActionAvro action) {
        jdbc.update("""
                INSERT INTO user_interactions(user_id, event_id, weight, updated_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (user_id, event_id) DO UPDATE SET
                  weight = GREATEST(user_interactions.weight, EXCLUDED.weight),
                  updated_at = GREATEST(user_interactions.updated_at, EXCLUDED.updated_at)
                """, action.getUserId(), action.getEventId(), ActionWeight.of(action.getActionType()),
                Timestamp.from(action.getTimestamp()));
    }

    @Transactional
    public void save(EventSimilarityAvro similarity) {
        jdbc.update("""
                INSERT INTO event_similarities(event_a, event_b, score, updated_at) VALUES (?, ?, ?, ?)
                ON CONFLICT (event_a, event_b) DO UPDATE SET score = EXCLUDED.score, updated_at = EXCLUDED.updated_at
                """, Math.min(similarity.getEventA(), similarity.getEventB()),
                Math.max(similarity.getEventA(), similarity.getEventB()), similarity.getScore(),
                Timestamp.from(similarity.getTimestamp()));
    }

    public List<Interaction> history(long userId) {
        return jdbc.query("""
                SELECT event_id, weight, updated_at FROM user_interactions
                WHERE user_id = ? ORDER BY updated_at DESC, event_id
                """, (row, index) -> new Interaction(row.getLong(1), row.getDouble(2),
                row.getTimestamp(3).toInstant()), userId);
    }

    public List<Similarity> similar(long eventId) {
        return jdbc.query("""
                SELECT event_a, event_b, score FROM event_similarities
                WHERE (event_a = ? OR event_b = ?) AND score > 0
                ORDER BY score DESC, event_a, event_b
                """, (row, index) -> new Similarity(row.getLong(1), row.getLong(2), row.getDouble(3)),
                eventId, eventId);
    }

    public double interactions(long eventId) {
        Double sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(weight), 0) FROM user_interactions WHERE event_id = ?", Double.class, eventId);
        return sum == null ? 0 : sum;
    }
}

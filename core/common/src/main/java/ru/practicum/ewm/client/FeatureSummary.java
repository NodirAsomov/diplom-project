package ru.practicum.ewm.client;

import lombok.*;
import ru.practicum.ewm.rating.dto.RatingStatsDto;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureSummary {
    private Map<Long, RatingStatsDto> ratings;
    private Map<Long, Long> comments;
    private Set<Long> followed;
}

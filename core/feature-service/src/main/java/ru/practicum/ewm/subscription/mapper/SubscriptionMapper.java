package ru.practicum.ewm.subscription.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.subscription.dto.SubscriptionDto;
import ru.practicum.ewm.subscription.model.Subscription;
import ru.practicum.ewm.user.dto.UserShortDto;

@Component
public class SubscriptionMapper {

    public SubscriptionDto toSubscriptionDto(Subscription subscription) {
        return SubscriptionDto.builder()
                .id(subscription.getId())
                .follower(new UserShortDto(subscription.getFollowerId(), subscription.getFollowerName()))
                .followed(new UserShortDto(subscription.getFollowedId(), subscription.getFollowedName()))
                .status(subscription.getStatus().toString())
                .created(subscription.getCreated().toString())
                .build();
    }
}

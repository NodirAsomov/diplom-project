package ru.practicum.ewm.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.subscription.dto.SubscriptionDto;
import ru.practicum.ewm.subscription.dto.SubscriptionInputDto;
import ru.practicum.ewm.subscription.dto.SubscriptionStatusUpdateResult;
import ru.practicum.ewm.subscription.mapper.SubscriptionMapper;
import ru.practicum.ewm.subscription.model.Subscription;
import ru.practicum.ewm.subscription.model.SubscriptionStatus;
import ru.practicum.ewm.subscription.repository.SubscriptionRepository;
import ru.practicum.ewm.subscription.exception.SubscriptionNotFoundException;
import ru.practicum.ewm.user.dto.UserShortDto;
import ru.practicum.ewm.user.exception.UserNotFoundException;

import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.client.UserLookup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserLookup userRepository;
    private final SubscriptionMapper mapper;


    @Override
    @Transactional
    public SubscriptionDto create(Long userId, SubscriptionInputDto dto) {
        UserDto follower = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        UserDto followed = userRepository.findById(dto.getTargetUserId())
                .orElseThrow(() -> new UserNotFoundException(dto.getTargetUserId()));

        if (follower.getId().equals(followed.getId())) {
            throw new ConflictException("Cannot subscribe to yourself");
        }

        if (subscriptionRepository.existsByFollowerIdAndFollowedId(follower.getId(), followed.getId())) {
            throw new ConflictException("Subscription already exists");
        }

        Subscription subscription = Subscription.builder()
                .followerId(follower.getId()).followerName(follower.getName())
                .followedId(followed.getId()).followedName(followed.getName())
                .status(SubscriptionStatus.PENDING)
                .created(LocalDateTime.now())
                .build();

        subscription = subscriptionRepository.save(subscription);
        log.debug("Subscription created: followerId={}, followedId={}", follower.getId(), followed.getId());
        return mapper.toSubscriptionDto(subscription);
    }

    @Override
    @Transactional
    public SubscriptionStatusUpdateResult confirm(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found"));

        if (!subscription.getFollowedId().equals(userId)) {
            throw new SubscriptionNotFoundException("Subscription not found");
        }

        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new ConflictException("Subscription must be in PENDING status");
        }

        subscription.setStatus(SubscriptionStatus.CONFIRMED);
        subscriptionRepository.save(subscription);
        log.debug("Subscription confirmed: {}", subscriptionId);
        return new SubscriptionStatusUpdateResult(subscription.getId(), subscription.getStatus().toString());
    }

    @Override
    @Transactional
    public SubscriptionStatusUpdateResult reject(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found"));

        if (!subscription.getFollowedId().equals(userId)) {
            throw new SubscriptionNotFoundException("Subscription not found");
        }

        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new ConflictException("Subscription must be in PENDING status");
        }

        subscription.setStatus(SubscriptionStatus.REJECTED);
        subscriptionRepository.save(subscription);
        log.debug("Subscription rejected: {}", subscriptionId);
        return new SubscriptionStatusUpdateResult(subscription.getId(), subscription.getStatus().toString());
    }

    @Override
    public List<SubscriptionDto> getAllByUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<Subscription> subscriptions = subscriptionRepository.findByFollowerId(userId);
        return subscriptions.stream()
                .map(mapper::toSubscriptionDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionDto> getIncoming(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<Subscription> subscriptions = subscriptionRepository
                .findByFollowedIdAndStatus(userId, SubscriptionStatus.PENDING);
        return subscriptions.stream()
                .map(mapper::toSubscriptionDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionDto> getOutgoing(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<Subscription> subscriptions = subscriptionRepository
                .findByFollowerIdAndStatus(userId, SubscriptionStatus.PENDING);
        return subscriptions.stream()
                .map(mapper::toSubscriptionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found"));

        if (!subscription.getFollowerId().equals(userId)) {
            throw new SubscriptionNotFoundException("Subscription not found");
        }

        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(subscription);
        log.debug("Subscription cancelled: {}", subscriptionId);
    }

    @Override
    public List<UserShortDto> getFollowers(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<UserShortDto> followers = subscriptionRepository
                .getFollowers(userId)
                .stream()
                .map(sub -> new UserShortDto(sub.getFollowerId(), sub.getFollowerName()))
                .toList();


        return followers;
    }
}

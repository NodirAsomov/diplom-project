package ru.practicum.ewm.infra;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RegistryStartupHealthIndicator implements ReactiveHealthIndicator {
    private static final List<String> REQUIRED_SERVICES = List.of(
            "event-service", "request-service", "user-service", "feature-service", "stats-server");
    private final ReactiveDiscoveryClient discovery;
    private final AtomicBoolean initialized = new AtomicBoolean();

    public RegistryStartupHealthIndicator(ReactiveDiscoveryClient discovery) {
        this.discovery = discovery;
    }

    @Override
    public Mono<Health> health() {
        if (initialized.get()) {
            return Mono.just(Health.up().build());
        }
        return Flux.fromIterable(REQUIRED_SERVICES)
                .flatMap(service -> discovery.getInstances(service).hasElements()
                        .filter(found -> !found).map(found -> service))
                .collectList()
                .map(missing -> {
                    if (missing.isEmpty()) {
                        initialized.set(true);
                        return Health.up().build();
                    }
                    return Health.down().withDetail("waitingForServices", missing).build();
                })
                .onErrorResume(error -> Mono.just(Health.down(error).build()));
    }
}

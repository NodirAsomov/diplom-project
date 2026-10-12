package ru.practicum.ewm.client;

import feign.FeignException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ReadResilience {
    private final CircuitBreakerFactory<?, ?> breakers;
    private final Retry retry = Retry.of("read", RetryConfig.custom()
            .maxAttempts(2).waitDuration(Duration.ofMillis(100))
            .retryOnException(error -> !(error instanceof FeignException)
                    || ((FeignException) error).status() < 0
                    || ((FeignException) error).status() >= 500).build());

    public <T> T read(String name, Supplier<T> call, Supplier<T> fallback) {
        return breakers.create(name).run(Retry.decorateSupplier(retry, call), error -> fallback.get());
    }

    public void bestEffort(String name, Runnable operation) {
        breakers.create(name).run(() -> {
            operation.run();
            return true;
        }, error -> false);
    }
}

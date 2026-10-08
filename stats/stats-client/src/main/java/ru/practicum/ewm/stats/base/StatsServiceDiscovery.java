package ru.practicum.ewm.stats.base;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
public class StatsServiceDiscovery {
    private final DiscoveryClient discoveryClient;
    private final String serviceId;
    private final RetryTemplate retryTemplate;

    public StatsServiceDiscovery(DiscoveryClient discoveryClient,
                                 @Value("${stats-server.service-id:stats-server}") String serviceId) {
        this.discoveryClient = discoveryClient;
        this.serviceId = serviceId;
        this.retryTemplate = RetryTemplate.builder().maxAttempts(3).fixedBackoff(3000L).build();
    }

    public URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance());
        return UriComponentsBuilder.fromUri(instance.getUri()).path(path).build().toUri();
    }

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances.isEmpty()) {
                throw new IllegalStateException("No registered instances");
            }
            return instances.getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + serviceId, exception);
        }
    }
}

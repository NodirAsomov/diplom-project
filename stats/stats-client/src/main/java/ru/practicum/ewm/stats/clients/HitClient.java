package ru.practicum.ewm.stats.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.base.BaseClient;
import ru.practicum.ewm.stats.base.StatsServiceDiscovery;
import ru.practicum.ewm.stats.dto.StatHitRequest;

import static ru.practicum.ewm.stats.base.ClientConstants.API_PREFIX_HIT;

@Slf4j
@Component
public class HitClient extends BaseClient {

    private final StatsServiceDiscovery discovery;

    public HitClient(RestTemplateBuilder builder, StatsServiceDiscovery discovery) {
        super(
                builder
                        .requestFactory(() -> new JdkClientHttpRequestFactory())
                        .build()
        );
        this.discovery = discovery;
    }

    public void saveHit(StatHitRequest request) {
        log.debug("Post saveHit request: {}", request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StatHitRequest> entity = new HttpEntity<>(request, headers);

        rest.postForEntity(discovery.makeUri(API_PREFIX_HIT), entity, Void.class);
    }
}

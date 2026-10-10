package ru.practicum.ewm.stats;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.practicum.ewm.stats.base.StatsServiceDiscovery;
import ru.practicum.ewm.stats.base.StatsServerUnavailable;
import ru.practicum.ewm.stats.clients.StatsClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StatsDiscoveryClientTest {
    @Test
    void resolvesCurrentAddressForEveryCall() {
        DiscoveryClient discovery = mock(DiscoveryClient.class);
        when(discovery.getInstances("stats-server")).thenReturn(
                List.of(new DefaultServiceInstance("one", "stats-server", "localhost", 1234, false)),
                List.of(new DefaultServiceInstance("two", "stats-server", "localhost", 5678, false)));
        StatsServiceDiscovery resolver = new StatsServiceDiscovery(discovery, "stats-server");

        assertEquals(URI.create("http://localhost:1234/hit"), resolver.makeUri("/hit"));
        assertEquals(URI.create("http://localhost:5678/stats"), resolver.makeUri("/stats"));
    }

    @Test
    void retriesUntilInstanceRegisters() {
        DiscoveryClient discovery = mock(DiscoveryClient.class);
        when(discovery.getInstances("stats-server")).thenReturn(List.of(),
                List.of(new DefaultServiceInstance("one", "stats-server", "localhost", 1234, false)));
        assertEquals(URI.create("http://localhost:1234/hit"),
                new StatsServiceDiscovery(discovery, "stats-server").makeUri("/hit"));
        verify(discovery, times(2)).getInstances("stats-server");
    }

    @Test
    void reportsUnavailableServiceAfterThreeAttempts() {
        DiscoveryClient discovery = mock(DiscoveryClient.class);
        when(discovery.getInstances("stats-server")).thenReturn(List.of());
        assertThrows(StatsServerUnavailable.class,
                () -> new StatsServiceDiscovery(discovery, "stats-server").makeUri("/hit"));
        verify(discovery, times(3)).getInstances("stats-server");
    }

    @Test
    void sendsEncodedQueryToDiscoveredServer() {
        StatsServiceDiscovery discovery = mock(StatsServiceDiscovery.class);
        when(discovery.makeUri("/stats")).thenReturn(URI.create("http://localhost:1234/stats"));
        RestTemplate rest = new RestTemplate();
        RestTemplateBuilder builder = mock(RestTemplateBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(rest);
        StatsClient client = new StatsClient(builder, discovery);
        MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
        server.expect(requestTo("http://localhost:1234/stats?start=2026-10-08%2000:00:00"
                + "&end=2026-10-09%2000:00:00&unique=true&uris=/events/1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getStats(LocalDateTime.of(2026, 10, 8, 0, 0),
                LocalDateTime.of(2026, 10, 9, 0, 0), List.of("/events/1"), true).isEmpty());
        server.verify();
    }
}

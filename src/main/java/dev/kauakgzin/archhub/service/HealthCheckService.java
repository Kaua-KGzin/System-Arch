package dev.kauakgzin.archhub.service;

import dev.kauakgzin.archhub.domain.RegisteredSystem;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/**
 * Actively probes every registered system that exposes a health check URL,
 * so status in the Hub stays accurate even for systems that never call the
 * heartbeat endpoint themselves.
 */
@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final SystemRegistry registry;
    private final RestClient restClient;
    private final Clock clock;

    public HealthCheckService(SystemRegistry registry, RestClient healthCheckRestClient, Clock clock) {
        this.registry = registry;
        this.restClient = healthCheckRestClient;
        this.clock = clock;
    }

    @Scheduled(fixedRateString = "${archhub.health.check-interval-ms:15000}")
    public void checkAll() {
        for (RegisteredSystem system : registry.findAll()) {
            if (system.getHealthCheckUrl() != null) {
                checkOne(system);
            }
        }
    }

    private void checkOne(RegisteredSystem system) {
        try {
            restClient.get()
                    .uri(system.getHealthCheckUrl())
                    .retrieve()
                    .toBodilessEntity();
            system.markSeen(clock.instant());
        } catch (Exception ex) {
            system.markCheckFailed(ex.getMessage());
            log.debug("Health check failed for system '{}': {}", system.getId(), ex.getMessage());
        }
    }
}

package dev.kauakgzin.archhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "archhub.health")
public record HubProperties(
        long checkIntervalMs,
        Duration staleAfter,
        Duration timeout
) {

    public HubProperties {
        if (checkIntervalMs <= 0) {
            checkIntervalMs = 15000;
        }
        if (staleAfter == null) {
            staleAfter = Duration.ofSeconds(45);
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(3);
        }
    }
}

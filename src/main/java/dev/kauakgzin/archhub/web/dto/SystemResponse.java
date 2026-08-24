package dev.kauakgzin.archhub.web.dto;

import dev.kauakgzin.archhub.domain.RegisteredSystem;
import dev.kauakgzin.archhub.domain.SystemStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record SystemResponse(
        String id,
        String name,
        String baseUrl,
        String healthCheckUrl,
        String description,
        List<String> tags,
        SystemStatus status,
        Instant registeredAt,
        Instant lastSeen,
        String lastError
) {

    public static SystemResponse of(RegisteredSystem system, Instant now, Duration staleAfter) {
        return new SystemResponse(
                system.getId(),
                system.getName(),
                system.getBaseUrl(),
                system.getHealthCheckUrl(),
                system.getDescription(),
                system.getTags(),
                system.statusAt(now, staleAfter),
                system.getRegisteredAt(),
                system.getLastSeen(),
                system.getLastError()
        );
    }
}

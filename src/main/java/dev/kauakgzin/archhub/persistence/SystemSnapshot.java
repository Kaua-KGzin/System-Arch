package dev.kauakgzin.archhub.persistence;

import dev.kauakgzin.archhub.domain.RegisteredSystem;

import java.time.Instant;
import java.util.List;

/**
 * On-disk representation of a {@link RegisteredSystem}, used to survive
 * Hub restarts without requiring a full database.
 */
public record SystemSnapshot(
        String id,
        String name,
        String baseUrl,
        String healthCheckUrl,
        String description,
        List<String> tags,
        List<String> connectsTo,
        Instant registeredAt,
        Instant lastSeen
) {

    public static SystemSnapshot of(RegisteredSystem system) {
        return new SystemSnapshot(
                system.getId(),
                system.getName(),
                system.getBaseUrl(),
                system.getHealthCheckUrl(),
                system.getDescription(),
                system.getTags(),
                system.getConnectsTo(),
                system.getRegisteredAt(),
                system.getLastSeen()
        );
    }

    public RegisteredSystem toDomain() {
        RegisteredSystem system = new RegisteredSystem(
                id, name, baseUrl, healthCheckUrl, description,
                tags == null ? List.of() : tags,
                connectsTo == null ? List.of() : connectsTo,
                registeredAt
        );
        if (lastSeen != null) {
            system.markSeen(lastSeen);
        }
        return system;
    }
}

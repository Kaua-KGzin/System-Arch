package dev.kauakgzin.archhub.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A system connected to the Arch Hub (e.g. System-PVD, ArchMAP, SIMPLE-ArCh).
 * Mutable fields ({@code lastSeen}, {@code lastError}) are updated concurrently
 * by both the scheduled health checker and inbound heartbeat requests.
 */
public class RegisteredSystem {

    private final String id;
    private final String name;
    private final String baseUrl;
    private final String healthCheckUrl;
    private final String description;
    private final List<String> tags;
    private final Instant registeredAt;

    private final AtomicReference<Instant> lastSeen = new AtomicReference<>();
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public RegisteredSystem(String id, String name, String baseUrl, String healthCheckUrl,
                             String description, List<String> tags, Instant registeredAt) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.healthCheckUrl = healthCheckUrl;
        this.description = description;
        this.tags = tags;
        this.registeredAt = registeredAt;
    }

    public void markSeen(Instant when) {
        lastSeen.updateAndGet(current -> current == null || when.isAfter(current) ? when : current);
        lastError.set(null);
    }

    public void markCheckFailed(String error) {
        lastError.set(error);
    }

    public SystemStatus statusAt(Instant now, Duration staleAfter) {
        Instant seen = lastSeen.get();
        if (seen == null) {
            return SystemStatus.UNKNOWN;
        }
        return Duration.between(seen, now).compareTo(staleAfter) <= 0 ? SystemStatus.UP : SystemStatus.DOWN;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getHealthCheckUrl() {
        return healthCheckUrl;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTags() {
        return tags;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getLastSeen() {
        return lastSeen.get();
    }

    public String getLastError() {
        return lastError.get();
    }
}

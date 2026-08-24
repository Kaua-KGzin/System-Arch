package dev.kauakgzin.archhub.service;

import dev.kauakgzin.archhub.config.HubProperties;
import dev.kauakgzin.archhub.domain.EventType;
import dev.kauakgzin.archhub.domain.RegisteredSystem;
import dev.kauakgzin.archhub.domain.SystemStatus;
import dev.kauakgzin.archhub.exception.SystemNotFoundException;
import dev.kauakgzin.archhub.repository.EventLog;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import dev.kauakgzin.archhub.web.dto.ConnectionResponse;
import dev.kauakgzin.archhub.web.dto.RegisterSystemRequest;
import dev.kauakgzin.archhub.web.dto.StatsResponse;
import dev.kauakgzin.archhub.web.dto.SystemResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class RegistrationService {

    private final SystemRegistry registry;
    private final HubProperties properties;
    private final Clock clock;
    private final EventLog eventLog;

    public RegistrationService(SystemRegistry registry, HubProperties properties, Clock clock, EventLog eventLog) {
        this.registry = registry;
        this.properties = properties;
        this.clock = clock;
        this.eventLog = eventLog;
    }

    public SystemResponse register(RegisterSystemRequest request) {
        Instant now = clock.instant();
        boolean isNew = registry.findById(request.id()).isEmpty();
        Instant registeredAt = registry.findById(request.id())
                .map(RegisteredSystem::getRegisteredAt)
                .orElse(now);

        List<String> tags = request.tags() == null ? List.of() : List.copyOf(request.tags());
        List<String> connectsTo = request.connectsTo() == null ? List.of() : List.copyOf(request.connectsTo());

        RegisteredSystem system = new RegisteredSystem(
                request.id(),
                request.name(),
                normalizeUrl(request.baseUrl()),
                normalizeOptionalUrl(request.healthCheckUrl()),
                request.description(),
                tags,
                connectsTo,
                registeredAt
        );
        // A system that just (re-)registered itself is reachable by definition.
        system.markSeen(now);

        registry.save(system);
        eventLog.append(system.getId(), EventType.REGISTERED,
                isNew ? "%s se registrou no Hub".formatted(system.getName())
                        : "%s atualizou seu registro".formatted(system.getName()),
                now);
        return toResponse(system, now);
    }

    public SystemResponse heartbeat(String id) {
        RegisteredSystem system = registry.getOrThrow(id);
        Instant now = clock.instant();
        system.markSeen(now);
        return toResponse(system, now);
    }

    public SystemResponse get(String id) {
        return toResponse(registry.getOrThrow(id), clock.instant());
    }

    public List<SystemResponse> list() {
        return list(null, null, null);
    }

    public List<SystemResponse> list(String tag, SystemStatus status, String query) {
        Instant now = clock.instant();
        String normalizedTag = normalize(tag);
        String normalizedQuery = normalize(query);

        return registry.findAll().stream()
                .filter(system -> normalizedTag == null || system.getTags().stream()
                        .anyMatch(t -> t.equalsIgnoreCase(normalizedTag)))
                .filter(system -> normalizedQuery == null || matches(system, normalizedQuery))
                .map(system -> toResponse(system, now))
                .filter(response -> status == null || response.status() == status)
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .collect(Collectors.toList());
    }

    public void deregister(String id) {
        if (!registry.deleteById(id)) {
            throw new SystemNotFoundException(id);
        }
        eventLog.append(id, EventType.DEREGISTERED, "%s foi removido do Hub".formatted(id), clock.instant());
    }

    public List<ConnectionResponse> connections() {
        List<ConnectionResponse> result = new ArrayList<>();
        for (RegisteredSystem system : registry.findAll()) {
            for (String target : system.getConnectsTo()) {
                result.add(new ConnectionResponse(system.getId(), target, registry.findById(target).isPresent()));
            }
        }
        return result;
    }

    public StatsResponse stats() {
        Collection<SystemResponse> systems = list();
        int up = 0;
        int down = 0;
        int unknown = 0;
        for (SystemResponse system : systems) {
            switch (system.status()) {
                case UP -> up++;
                case DOWN -> down++;
                case UNKNOWN -> unknown++;
            }
        }
        return new StatsResponse(systems.size(), up, down, unknown, connections().size());
    }

    private SystemResponse toResponse(RegisteredSystem system, Instant now) {
        return SystemResponse.of(system, now, properties.staleAfter());
    }

    private boolean matches(RegisteredSystem system, String query) {
        return contains(system.getId(), query) || contains(system.getName(), query) || contains(system.getDescription(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String normalizeOptionalUrl(String url) {
        return (url == null || url.isBlank()) ? null : normalizeUrl(url);
    }
}

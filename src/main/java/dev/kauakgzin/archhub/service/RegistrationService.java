package dev.kauakgzin.archhub.service;

import dev.kauakgzin.archhub.config.HubProperties;
import dev.kauakgzin.archhub.domain.RegisteredSystem;
import dev.kauakgzin.archhub.exception.SystemNotFoundException;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import dev.kauakgzin.archhub.web.dto.RegisterSystemRequest;
import dev.kauakgzin.archhub.web.dto.SystemResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistrationService {

    private final SystemRegistry registry;
    private final HubProperties properties;
    private final Clock clock;

    public RegistrationService(SystemRegistry registry, HubProperties properties, Clock clock) {
        this.registry = registry;
        this.properties = properties;
        this.clock = clock;
    }

    public SystemResponse register(RegisterSystemRequest request) {
        Instant now = clock.instant();
        Instant registeredAt = registry.findById(request.id())
                .map(RegisteredSystem::getRegisteredAt)
                .orElse(now);

        List<String> tags = request.tags() == null ? List.of() : List.copyOf(request.tags());

        RegisteredSystem system = new RegisteredSystem(
                request.id(),
                request.name(),
                normalizeUrl(request.baseUrl()),
                normalizeOptionalUrl(request.healthCheckUrl()),
                request.description(),
                tags,
                registeredAt
        );
        // A system that just (re-)registered itself is reachable by definition.
        system.markSeen(now);

        registry.save(system);
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
        Instant now = clock.instant();
        Collection<RegisteredSystem> systems = registry.findAll();
        return systems.stream()
                .map(system -> toResponse(system, now))
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .collect(Collectors.toList());
    }

    public void deregister(String id) {
        if (!registry.deleteById(id)) {
            throw new SystemNotFoundException(id);
        }
    }

    private SystemResponse toResponse(RegisteredSystem system, Instant now) {
        return SystemResponse.of(system, now, properties.staleAfter());
    }

    private String normalizeUrl(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String normalizeOptionalUrl(String url) {
        return (url == null || url.isBlank()) ? null : normalizeUrl(url);
    }
}

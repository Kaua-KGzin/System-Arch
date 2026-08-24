package dev.kauakgzin.archhub.repository;

import dev.kauakgzin.archhub.domain.RegisteredSystem;
import dev.kauakgzin.archhub.exception.SystemNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of connected systems, keyed by system id.
 * Registration is expected to be re-issued on every deploy/restart of a
 * connected system, so durability across Hub restarts is not required yet.
 */
@Repository
public class SystemRegistry {

    private final ConcurrentHashMap<String, RegisteredSystem> systems = new ConcurrentHashMap<>();

    public RegisteredSystem save(RegisteredSystem system) {
        systems.put(system.getId(), system);
        return system;
    }

    public Optional<RegisteredSystem> findById(String id) {
        return Optional.ofNullable(systems.get(id));
    }

    public RegisteredSystem getOrThrow(String id) {
        return findById(id).orElseThrow(() -> new SystemNotFoundException(id));
    }

    public Collection<RegisteredSystem> findAll() {
        return systems.values();
    }

    public boolean deleteById(String id) {
        return systems.remove(id) != null;
    }
}

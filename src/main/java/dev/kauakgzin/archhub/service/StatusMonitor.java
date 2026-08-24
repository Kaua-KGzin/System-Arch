package dev.kauakgzin.archhub.service;

import dev.kauakgzin.archhub.config.HubProperties;
import dev.kauakgzin.archhub.domain.EventType;
import dev.kauakgzin.archhub.domain.RegisteredSystem;
import dev.kauakgzin.archhub.domain.SystemStatus;
import dev.kauakgzin.archhub.repository.EventLog;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Watches every registered system's computed status and drops a
 * {@code WENT_UP}/{@code WENT_DOWN} event whenever it flips, so the
 * activity feed reflects reality even though status itself is derived
 * on read rather than stored.
 */
@Service
public class StatusMonitor {

    private final SystemRegistry registry;
    private final HubProperties properties;
    private final Clock clock;
    private final EventLog eventLog;
    private final ConcurrentMap<String, SystemStatus> lastKnownStatus = new ConcurrentHashMap<>();

    public StatusMonitor(SystemRegistry registry, HubProperties properties, Clock clock, EventLog eventLog) {
        this.registry = registry;
        this.properties = properties;
        this.clock = clock;
        this.eventLog = eventLog;
    }

    @Scheduled(fixedRateString = "${archhub.health.check-interval-ms:15000}")
    public void detectTransitions() {
        Instant now = clock.instant();
        Set<String> currentIds = new HashSet<>();

        for (RegisteredSystem system : registry.findAll()) {
            currentIds.add(system.getId());
            SystemStatus current = system.statusAt(now, properties.staleAfter());
            SystemStatus previous = lastKnownStatus.put(system.getId(), current);

            if (previous != null && previous != current
                    && (current == SystemStatus.UP || current == SystemStatus.DOWN)) {
                EventType type = current == SystemStatus.UP ? EventType.WENT_UP : EventType.WENT_DOWN;
                String message = current == SystemStatus.UP
                        ? "%s ficou online".formatted(system.getName())
                        : "%s parou de responder".formatted(system.getName());
                eventLog.append(system.getId(), type, message, now);
            }
        }

        lastKnownStatus.keySet().retainAll(currentIds);
    }
}

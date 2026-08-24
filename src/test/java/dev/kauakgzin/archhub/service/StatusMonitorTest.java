package dev.kauakgzin.archhub.service;

import dev.kauakgzin.archhub.config.HubProperties;
import dev.kauakgzin.archhub.domain.EventType;
import dev.kauakgzin.archhub.domain.RegisteredSystem;
import dev.kauakgzin.archhub.repository.EventLog;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class StatusMonitorTest {

    private final SystemRegistry registry = new SystemRegistry();
    private final EventLog eventLog = new EventLog();
    private final HubProperties properties = new HubProperties(15000, Duration.ofSeconds(45), Duration.ofSeconds(3));
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
    private final Clock clock = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };

    private StatusMonitor newMonitor() {
        return new StatusMonitor(registry, properties, clock, eventLog);
    }

    private RegisteredSystem system(String id) {
        RegisteredSystem system = new RegisteredSystem(id, "System " + id, "http://localhost", null,
                "desc", List.of(), List.of(), now.get());
        system.markSeen(now.get());
        return system;
    }

    @Test
    void doesNotPublishEventOnFirstObservation() {
        registry.save(system("archmap"));
        StatusMonitor monitor = newMonitor();

        monitor.detectTransitions();

        assertThat(eventLog.recent(10)).isEmpty();
    }

    @Test
    void publishesWentDownWhenSystemGoesStale() {
        RegisteredSystem system = system("archmap");
        registry.save(system);
        StatusMonitor monitor = newMonitor();
        monitor.detectTransitions();

        now.set(now.get().plus(Duration.ofMinutes(5)));
        monitor.detectTransitions();

        assertThat(eventLog.recent(10)).extracting(e -> e.type()).containsExactly(EventType.WENT_DOWN);
    }

    @Test
    void publishesWentUpWhenSystemRecoversAfterHeartbeat() {
        RegisteredSystem system = system("archmap");
        registry.save(system);
        StatusMonitor monitor = newMonitor();
        monitor.detectTransitions();

        now.set(now.get().plus(Duration.ofMinutes(5)));
        monitor.detectTransitions();

        now.set(now.get().plus(Duration.ofSeconds(1)));
        system.markSeen(now.get());
        monitor.detectTransitions();

        assertThat(eventLog.recent(10)).extracting(e -> e.type())
                .containsExactly(EventType.WENT_UP, EventType.WENT_DOWN);
    }
}

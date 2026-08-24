package dev.kauakgzin.archhub.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegisteredSystemTest {

    private RegisteredSystem newSystem() {
        return new RegisteredSystem(
                "archmap", "ArchMAP", "http://localhost:9000", "http://localhost:9000/health",
                "Code review graph", List.of("python", "cli"), Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    @Test
    void statusIsUnknownWhenNeverSeen() {
        RegisteredSystem system = newSystem();

        assertThat(system.statusAt(Instant.parse("2026-01-01T00:00:10Z"), Duration.ofSeconds(45)))
                .isEqualTo(SystemStatus.UNKNOWN);
    }

    @Test
    void statusIsUpWithinStaleWindow() {
        RegisteredSystem system = newSystem();
        Instant seenAt = Instant.parse("2026-01-01T00:00:00Z");
        system.markSeen(seenAt);

        assertThat(system.statusAt(seenAt.plusSeconds(30), Duration.ofSeconds(45)))
                .isEqualTo(SystemStatus.UP);
    }

    @Test
    void statusIsDownAfterStaleWindow() {
        RegisteredSystem system = newSystem();
        Instant seenAt = Instant.parse("2026-01-01T00:00:00Z");
        system.markSeen(seenAt);

        assertThat(system.statusAt(seenAt.plusSeconds(60), Duration.ofSeconds(45)))
                .isEqualTo(SystemStatus.DOWN);
    }

    @Test
    void markSeenIgnoresOlderTimestamps() {
        RegisteredSystem system = newSystem();
        Instant later = Instant.parse("2026-01-01T00:05:00Z");
        Instant earlier = Instant.parse("2026-01-01T00:00:00Z");

        system.markSeen(later);
        system.markSeen(earlier);

        assertThat(system.getLastSeen()).isEqualTo(later);
    }

    @Test
    void markSeenClearsLastError() {
        RegisteredSystem system = newSystem();
        system.markCheckFailed("connection refused");

        system.markSeen(Instant.now());

        assertThat(system.getLastError()).isNull();
    }
}

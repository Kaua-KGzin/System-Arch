package dev.kauakgzin.archhub.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.kauakgzin.archhub.config.HubPersistenceProperties;
import dev.kauakgzin.archhub.domain.ArchitectureReport;
import dev.kauakgzin.archhub.domain.RegisteredSystem;
import dev.kauakgzin.archhub.domain.SystemStatus;
import dev.kauakgzin.archhub.repository.ArchitectureStore;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void loadFromMissingFileReturnsEmptyList(@TempDir Path dir) {
        SystemRegistry registry = new SystemRegistry();
        PersistenceService service = new PersistenceService(registry, new HubPersistenceProperties(true, null), objectMapper, new ArchitectureStore());

        assertThat(service.loadFrom(dir.resolve("does-not-exist.json"))).isEmpty();
    }

    @Test
    void savingAndReloadingRoundTripsSystemFields(@TempDir Path dir) {
        SystemRegistry registry = new SystemRegistry();
        Instant registeredAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant lastSeen = Instant.parse("2026-01-01T01:00:00Z");
        RegisteredSystem system = new RegisteredSystem("archmap", "ArchMAP", "http://localhost:9000",
                "http://localhost:9000/health", "desc", List.of("python"), List.of("system-pvd"), registeredAt);
        system.markSeen(lastSeen);
        registry.save(system);

        PersistenceService service = new PersistenceService(registry, new HubPersistenceProperties(true, null), objectMapper, new ArchitectureStore());
        Path file = dir.resolve("systems.json");

        service.saveTo(file, registry.findAll());
        assertThat(Files.exists(file)).isTrue();

        List<RegisteredSystem> restored = service.loadFrom(file);

        assertThat(restored).hasSize(1);
        RegisteredSystem reloaded = restored.get(0);
        assertThat(reloaded.getId()).isEqualTo("archmap");
        assertThat(reloaded.getTags()).containsExactly("python");
        assertThat(reloaded.getConnectsTo()).containsExactly("system-pvd");
        assertThat(reloaded.getRegisteredAt()).isEqualTo(registeredAt);
        assertThat(reloaded.getLastSeen()).isEqualTo(lastSeen);
    }

    @Test
    void reloadedSystemStatusReflectsPersistedLastSeen(@TempDir Path dir) {
        SystemRegistry registry = new SystemRegistry();
        Instant lastSeen = Instant.parse("2026-01-01T00:00:00Z");
        RegisteredSystem system = new RegisteredSystem("archmap", "ArchMAP", "http://localhost:9000", null,
                null, List.of(), List.of(), lastSeen);
        system.markSeen(lastSeen);
        registry.save(system);

        PersistenceService service = new PersistenceService(registry, new HubPersistenceProperties(true, null), objectMapper, new ArchitectureStore());
        Path file = dir.resolve("systems.json");
        service.saveTo(file, registry.findAll());

        RegisteredSystem reloaded = service.loadFrom(file).get(0);

        // Far past its stale window: should read back as DOWN, not UP.
        assertThat(reloaded.statusAt(lastSeen.plus(Duration.ofDays(1)), Duration.ofSeconds(45)))
                .isEqualTo(SystemStatus.DOWN);
    }

    @Test
    void architectureReportsRoundTripNewestFirst(@TempDir Path dir) {
        ArchitectureStore store = new ArchitectureStore();
        store.add("archmap", report(70));
        store.add("archmap", report(82));
        PersistenceService service = new PersistenceService(new SystemRegistry(),
                new HubPersistenceProperties(true, null), objectMapper, store);
        Path file = dir.resolve("architecture.json");

        service.saveArchitecture(file);
        ArchitectureStore reloaded = new ArchitectureStore();
        new PersistenceService(new SystemRegistry(), new HubPersistenceProperties(true, null), objectMapper, reloaded)
                .loadArchitecture(file);

        assertThat(reloaded.history("archmap")).extracting(ArchitectureReport::score).containsExactly(82, 70);
    }

    @Test
    void architectureFileSitsNextToSystemsSnapshot() {
        PersistenceService service = new PersistenceService(new SystemRegistry(),
                new HubPersistenceProperties(true, "data/systems.json"), objectMapper, new ArchitectureStore());

        assertThat(service.architectureFile()).isEqualTo(Path.of("data/architecture.json"));
    }

    private static ArchitectureReport report(int score) {
        return new ArchitectureReport(score, "B", 10, 0, 1, "abc123", "main", "1.5.0", null,
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}

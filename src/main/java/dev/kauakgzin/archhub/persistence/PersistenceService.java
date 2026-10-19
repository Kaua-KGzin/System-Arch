package dev.kauakgzin.archhub.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.kauakgzin.archhub.config.HubPersistenceProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import dev.kauakgzin.archhub.domain.ArchitectureReport;
import dev.kauakgzin.archhub.domain.RegisteredSystem;
import dev.kauakgzin.archhub.repository.ArchitectureStore;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Snapshots the in-memory {@link SystemRegistry} to a JSON file on shutdown
 * and reloads it on startup, so a Hub restart doesn't forget every system
 * that had registered. This is deliberately a flat-file, not a database:
 * the catalog is small and re-registration on every connected system's
 * boot is expected anyway, so this only smooths over Hub-only restarts.
 */
@Component
public class PersistenceService implements ApplicationRunner, ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(PersistenceService.class);

    private final SystemRegistry registry;
    private final HubPersistenceProperties properties;
    private final ObjectMapper objectMapper;
    private final ArchitectureStore architectureStore;

    public PersistenceService(SystemRegistry registry, HubPersistenceProperties properties, ObjectMapper objectMapper,
                              ArchitectureStore architectureStore) {
        this.registry = registry;
        this.properties = properties;
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.architectureStore = architectureStore;
    }

    /** Architecture reports live next to the systems snapshot: data/systems.json → data/architecture.json. */
    Path architectureFile() {
        Path systems = Path.of(properties.file());
        return systems.resolveSibling("architecture.json");
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }
        List<RegisteredSystem> restored = loadFrom(Path.of(properties.file()));
        restored.forEach(registry::save);
        if (!restored.isEmpty()) {
            log.info("Restored {} system(s) from {}", restored.size(), properties.file());
        }
        loadArchitecture(architectureFile());
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if (!properties.enabled()) {
            return;
        }
        saveTo(Path.of(properties.file()), registry.findAll());
        saveArchitecture(architectureFile());
    }

    void loadArchitecture(Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            Map<String, List<ArchitectureReport>> reports = objectMapper.readValue(file.toFile(), new TypeReference<>() {
            });
            reports.forEach(architectureStore::restore);
        } catch (IOException ex) {
            log.warn("Could not read architecture file {}: {}", file, ex.getMessage());
        }
    }

    void saveArchitecture(Path file) {
        Map<String, List<ArchitectureReport>> reports = architectureStore.snapshot();
        if (reports.isEmpty() && !Files.exists(file)) {
            return;
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writeValue(file.toFile(), reports);
        } catch (IOException ex) {
            log.warn("Could not write architecture file {}: {}", file, ex.getMessage());
        }
    }

    List<RegisteredSystem> loadFrom(Path file) {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            SystemSnapshot[] snapshots = objectMapper.readValue(file.toFile(), SystemSnapshot[].class);
            return List.of(snapshots).stream().map(SystemSnapshot::toDomain).toList();
        } catch (IOException ex) {
            log.warn("Could not read snapshot file {}: {}", file, ex.getMessage());
            return List.of();
        }
    }

    void saveTo(Path file, Collection<RegisteredSystem> systems) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            List<SystemSnapshot> snapshots = systems.stream().map(SystemSnapshot::of).toList();
            objectMapper.writeValue(file.toFile(), snapshots);
            log.info("Saved {} system(s) to {}", snapshots.size(), file);
        } catch (IOException ex) {
            log.warn("Could not write snapshot file {}: {}", file, ex.getMessage());
        }
    }
}

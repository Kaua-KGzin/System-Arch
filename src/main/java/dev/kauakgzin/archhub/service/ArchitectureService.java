package dev.kauakgzin.archhub.service;

import dev.kauakgzin.archhub.domain.ArchitectureReport;
import dev.kauakgzin.archhub.domain.EventType;
import dev.kauakgzin.archhub.repository.ArchitectureStore;
import dev.kauakgzin.archhub.repository.EventLog;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import dev.kauakgzin.archhub.web.dto.ArchitectureEntry;
import dev.kauakgzin.archhub.web.dto.ArchitectureReportRequest;
import dev.kauakgzin.archhub.web.dto.ArchitectureSummary;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class ArchitectureService {

    private final ArchitectureStore store;
    private final SystemRegistry registry;
    private final EventLog eventLog;
    private final Clock clock;

    public ArchitectureService(ArchitectureStore store, SystemRegistry registry, EventLog eventLog, Clock clock) {
        this.store = store;
        this.registry = registry;
        this.eventLog = eventLog;
        this.clock = clock;
    }

    /** @throws IllegalStateException when the store is full and {@code systemId} is new. */
    public ArchitectureSummary report(String systemId, ArchitectureReportRequest request) {
        Instant now = clock.instant();
        ArchitectureReport report = new ArchitectureReport(
                request.score(), request.grade(), request.files(), request.cycles(), request.findings(),
                request.commit(), request.ref(), request.toolVersion(), request.reportUrl(), now);
        if (!store.add(systemId, report)) {
            throw new IllegalStateException("Architecture store is full");
        }
        ArchitectureSummary summary = summary(systemId);
        eventLog.append(systemId, EventType.ARCHITECTURE_REPORTED, describe(systemId, summary), now);
        return summary;
    }

    public ArchitectureSummary summary(String systemId) {
        return ArchitectureSummary.of(store.history(systemId));
    }

    public List<ArchitectureReport> history(String systemId) {
        return store.history(systemId);
    }

    public List<ArchitectureEntry> all() {
        return store.snapshot().keySet().stream()
                .sorted(Comparator.naturalOrder())
                .map(id -> new ArchitectureEntry(id, registry.findById(id).isPresent(), summary(id)))
                .toList();
    }

    private String describe(String systemId, ArchitectureSummary summary) {
        ArchitectureReport latest = summary.latest();
        String grade = latest.grade() == null ? "" : " (%s)".formatted(latest.grade());
        String delta = summary.delta() == null || summary.delta() == 0 ? ""
                : summary.delta() > 0 ? " ▲%d".formatted(summary.delta()) : " ▼%d".formatted(-summary.delta());
        return "%s: arquitetura %d/100%s%s".formatted(systemId, latest.score(), grade, delta);
    }
}

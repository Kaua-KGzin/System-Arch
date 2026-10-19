package dev.kauakgzin.archhub.repository;

import dev.kauakgzin.archhub.domain.ArchitectureReport;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Latest architecture reports per system id (newest first), capped both in
 * history length and in number of systems so an open Hub can't be filled
 * with junk ids.
 */
@Repository
public class ArchitectureStore {

    public static final int MAX_HISTORY = 20;
    public static final int MAX_SYSTEMS = 200;

    private final ConcurrentHashMap<String, Deque<ArchitectureReport>> reports = new ConcurrentHashMap<>();

    /** @return false when the store is full and {@code systemId} is new. */
    public boolean add(String systemId, ArchitectureReport report) {
        if (!reports.containsKey(systemId) && reports.size() >= MAX_SYSTEMS) {
            return false;
        }
        Deque<ArchitectureReport> history = reports.computeIfAbsent(systemId, id -> new ArrayDeque<>());
        synchronized (history) {
            history.addFirst(report);
            while (history.size() > MAX_HISTORY) {
                history.removeLast();
            }
        }
        return true;
    }

    public List<ArchitectureReport> history(String systemId) {
        Deque<ArchitectureReport> history = reports.get(systemId);
        if (history == null) {
            return List.of();
        }
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    public Optional<ArchitectureReport> latest(String systemId) {
        return history(systemId).stream().findFirst();
    }

    public Map<String, List<ArchitectureReport>> snapshot() {
        return reports.keySet().stream().collect(Collectors.toMap(id -> id, this::history));
    }

    public void restore(String systemId, List<ArchitectureReport> newestFirst) {
        reports.put(systemId, new ArrayDeque<>(newestFirst.stream().limit(MAX_HISTORY).toList()));
    }

    public boolean remove(String systemId) {
        return reports.remove(systemId) != null;
    }
}

package dev.kauakgzin.archhub.web.dto;

/** One row of {@code GET /api/v1/architecture}: every system with a report, registered or not. */
public record ArchitectureEntry(String systemId, boolean registered, ArchitectureSummary architecture) {
}

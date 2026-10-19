package dev.kauakgzin.archhub.web.dto;

import dev.kauakgzin.archhub.domain.ArchitectureReport;

import java.util.List;

/**
 * Latest report plus the score trend, embedded in {@link SystemResponse}.
 * {@code delta} compares with the previous report (null when there is none);
 * {@code trend} lists past scores oldest first, ready for a sparkline.
 */
public record ArchitectureSummary(
        ArchitectureReport latest,
        Integer delta,
        List<Integer> trend
) {

    public static ArchitectureSummary of(List<ArchitectureReport> newestFirst) {
        if (newestFirst.isEmpty()) {
            return null;
        }
        ArchitectureReport latest = newestFirst.get(0);
        Integer delta = newestFirst.size() > 1 ? latest.score() - newestFirst.get(1).score() : null;
        List<Integer> trend = newestFirst.reversed().stream().map(ArchitectureReport::score).toList();
        return new ArchitectureSummary(latest, delta, trend);
    }
}

package dev.kauakgzin.archhub.domain;

import java.time.Instant;

/**
 * One architecture analysis of a system, as published by ArchMAP
 * ({@code archmap hub-report}), usually from CI. Kept independent from
 * {@link RegisteredSystem} because CI reports a repository even when the
 * service itself is not running.
 */
public record ArchitectureReport(
        int score,
        String grade,
        int files,
        int cycles,
        int findings,
        String commit,
        String ref,
        String toolVersion,
        String reportUrl,
        Instant analyzedAt
) {
}

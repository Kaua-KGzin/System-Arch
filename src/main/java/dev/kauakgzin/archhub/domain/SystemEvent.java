package dev.kauakgzin.archhub.domain;

import java.time.Instant;

public record SystemEvent(
        long sequence,
        String systemId,
        EventType type,
        String message,
        Instant occurredAt
) {
}

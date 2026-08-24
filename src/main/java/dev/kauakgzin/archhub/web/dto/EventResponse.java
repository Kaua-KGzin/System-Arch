package dev.kauakgzin.archhub.web.dto;

import dev.kauakgzin.archhub.domain.EventType;
import dev.kauakgzin.archhub.domain.SystemEvent;

import java.time.Instant;

public record EventResponse(
        long sequence,
        String systemId,
        EventType type,
        String message,
        Instant occurredAt
) {

    public static EventResponse of(SystemEvent event) {
        return new EventResponse(event.sequence(), event.systemId(), event.type(), event.message(), event.occurredAt());
    }
}

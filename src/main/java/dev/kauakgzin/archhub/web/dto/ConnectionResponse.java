package dev.kauakgzin.archhub.web.dto;

/**
 * One edge in the connection graph: {@code from} calls/depends on {@code to}.
 * {@code toKnown} is false when {@code to} references a system id that
 * hasn't (or hasn't yet) registered itself in the Hub.
 */
public record ConnectionResponse(
        String from,
        String to,
        boolean toKnown
) {
}

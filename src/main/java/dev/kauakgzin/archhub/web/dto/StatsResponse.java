package dev.kauakgzin.archhub.web.dto;

public record StatsResponse(
        int total,
        int up,
        int down,
        int unknown,
        int connections
) {
}

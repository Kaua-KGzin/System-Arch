package dev.kauakgzin.archhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "archhub.persistence")
public record HubPersistenceProperties(boolean enabled, String file) {

    public HubPersistenceProperties {
        if (file == null || file.isBlank()) {
            file = "data/systems.json";
        }
    }
}

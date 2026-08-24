package dev.kauakgzin.archhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared-secret used to protect mutating endpoints (register/heartbeat/deregister).
 * When {@code token} is blank, the Hub stays open (dev-friendly default) and no
 * {@code X-Hub-Token} header is required.
 */
@ConfigurationProperties(prefix = "archhub.security")
public record HubSecurityProperties(String token) {

    public boolean enabled() {
        return token != null && !token.isBlank();
    }
}

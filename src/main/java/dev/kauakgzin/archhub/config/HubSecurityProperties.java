package dev.kauakgzin.archhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Secrets protecting mutating endpoints (register/heartbeat/architecture/deregister).
 *
 * <ul>
 *   <li>{@code token} — global admin secret, valid for any system.</li>
 *   <li>{@code systemTokens} — one secret per system, as {@code "id=token,id2=token2"}
 *       (env {@code ARCHHUB_SYSTEM_TOKENS}). A system token only writes its own id,
 *       so a leaked token can't overwrite or delete the rest of the line.</li>
 * </ul>
 * With neither configured the Hub stays open (dev-friendly default).
 */
@ConfigurationProperties(prefix = "archhub.security")
public record HubSecurityProperties(String token, String systemTokens) {

    @ConstructorBinding
    public HubSecurityProperties {
    }

    public HubSecurityProperties(String token) {
        this(token, null);
    }

    public boolean enabled() {
        return hasText(token) || !parsedSystemTokens().isEmpty();
    }

    public boolean isAdmin(String provided) {
        return hasText(token) && constantTimeEquals(token, provided);
    }

    /** @return the system id owning {@code provided}, or null. Compares against every entry in constant time. */
    public String systemFor(String provided) {
        String match = null;
        for (Map.Entry<String, String> entry : parsedSystemTokens().entrySet()) {
            if (constantTimeEquals(entry.getValue(), provided)) {
                match = entry.getKey();
            }
        }
        return match;
    }

    public Map<String, String> parsedSystemTokens() {
        Map<String, String> result = new LinkedHashMap<>();
        if (!hasText(systemTokens)) {
            return result;
        }
        for (String pair : systemTokens.split(",")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String id = pair.substring(0, eq).trim();
            String token = pair.substring(eq + 1).trim();
            // "archmap= " must not turn an empty X-Hub-Token header into archmap's credential.
            if (!id.isEmpty() && !token.isEmpty()) {
                result.put(id, token);
            }
        }
        return result;
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        if (provided == null || provided.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

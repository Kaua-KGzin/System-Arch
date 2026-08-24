package dev.kauakgzin.archhub.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Payload a connected system sends to register (or re-register) itself in the Hub.
 * {@code id} is a stable slug, e.g. "system-pvd", "archmap", "simple-arch".
 */
public record RegisterSystemRequest(

        @NotBlank
        @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,62})$", message = "id must be a lowercase slug, e.g. 'system-pvd'")
        String id,

        @NotBlank
        String name,

        @NotBlank
        String baseUrl,

        String healthCheckUrl,

        String description,

        List<String> tags,

        /**
         * Ids of other systems this one talks to. Used to draw the
         * connection graph; targets do not need to be registered yet.
         */
        List<String> connectsTo
) {
}

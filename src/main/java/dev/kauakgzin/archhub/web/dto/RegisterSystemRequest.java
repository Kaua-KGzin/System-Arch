package dev.kauakgzin.archhub.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
        @Size(max = 100)
        String name,

        @NotBlank
        @Pattern(regexp = HTTP_URL, message = "baseUrl must be an http(s) URL")
        String baseUrl,

        @Pattern(regexp = "^$|" + HTTP_URL, message = "healthCheckUrl must be an http(s) URL")
        String healthCheckUrl,

        @Size(max = 500)
        String description,

        @Size(max = 20)
        List<@Size(max = 40) String> tags,

        /**
         * Ids of other systems this one talks to. Used to draw the
         * connection graph; targets do not need to be registered yet.
         */
        @Size(max = 20)
        List<@Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{0,62})$") String> connectsTo
) {

    /** Rejects javascript:, data:, file: etc. — the dashboard renders baseUrl as a link. */
    static final String HTTP_URL = "^https?://[^\\s]+$";
}

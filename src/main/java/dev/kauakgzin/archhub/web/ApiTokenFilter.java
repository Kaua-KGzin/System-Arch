package dev.kauakgzin.archhub.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kauakgzin.archhub.config.HubSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Guards write operations under {@code /api/v1/systems} with {@code X-Hub-Token}
 * when a global or per-system token is configured (see {@link HubSecurityProperties}).
 * Reads (GET) always stay open so the dashboard keeps working without credentials.
 *
 * <p>A per-system token may only touch {@code /api/v1/systems/{itsOwnId}/**}. For
 * {@code POST /api/v1/systems} the id lives in the body, so the filter exposes the
 * authenticated id as the {@link #AUTHENTICATED_SYSTEM} request attribute and the
 * controller enforces it.
 *
 * <p>Registered explicitly as a {@link org.springframework.boot.web.servlet.FilterRegistrationBean}
 * in {@code HubConfig} (not {@code @Component}) so it isn't auto-picked up by
 * {@code @WebMvcTest} slices that don't load the full configuration.
 */
public class ApiTokenFilter extends OncePerRequestFilter {

    public static final String AUTHENTICATED_SYSTEM = "archhub.authenticatedSystem";

    private static final String HEADER = "X-Hub-Token";
    private static final String SYSTEMS_PREFIX = "/api/v1/systems/";
    private static final Set<String> PROTECTED_METHODS = Set.of(
            HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.DELETE.name(), HttpMethod.PATCH.name());

    private final HubSecurityProperties properties;
    private final ObjectMapper objectMapper;

    public ApiTokenFilter(HubSecurityProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!requiresToken(request)) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        if (properties.isAdmin(provided)) {
            chain.doFilter(request, response);
            return;
        }

        String system = properties.systemFor(provided);
        if (system == null) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid " + HEADER + " header");
            return;
        }

        String pathId = pathSystemId(request.getRequestURI());
        if (pathId != null && !pathId.equals(system)) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "Token of '%s' cannot modify '%s'".formatted(system, pathId));
            return;
        }
        request.setAttribute(AUTHENTICATED_SYSTEM, system);
        chain.doFilter(request, response);
    }

    /** "/api/v1/systems/archmap/heartbeat" → "archmap"; "/api/v1/systems" → null. */
    static String pathSystemId(String uri) {
        if (!uri.startsWith(SYSTEMS_PREFIX)) {
            return null;
        }
        String rest = uri.substring(SYSTEMS_PREFIX.length());
        int slash = rest.indexOf('/');
        String id = slash < 0 ? rest : rest.substring(0, slash);
        return id.isEmpty() ? null : id;
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", Instant.now().toString(),
                "message", message
        ));
    }

    private boolean requiresToken(HttpServletRequest request) {
        return properties.enabled()
                && request.getRequestURI().startsWith("/api/v1/systems")
                && PROTECTED_METHODS.contains(request.getMethod());
    }
}

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
import org.springframework.web.util.UrlPathHelper;

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
    private static final String SYSTEMS_ROOT = "/api/v1/systems";
    private static final String SYSTEMS_PREFIX = SYSTEMS_ROOT + "/";
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

        if (!properties.enabled() || !PROTECTED_METHODS.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Spring MVC routes on the normalized path (";params" stripped, %-decoded, "//" collapsed),
        // so authorization must decide on that same path. Deciding on the raw URI let
        // "/api/v1;x/systems/{id}" skip the token entirely and "/api/v1/systems;x/{id}/..." escape
        // the per-system check. Writes never need matrix params: refuse them outright.
        if (request.getRequestURI().indexOf(';') >= 0) {
            reject(response, HttpServletResponse.SC_BAD_REQUEST, "Path parameters (;) are not accepted on write requests");
            return;
        }
        String path = UrlPathHelper.defaultInstance.getLookupPathForRequest(request);
        if (!isSystemsPath(path)) {
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

        // A system token may register (POST /api/v1/systems, body id checked by the controller)
        // or write under its own id — nothing else. Unknown shapes fail closed.
        String pathId = pathSystemId(path);
        boolean register = SYSTEMS_ROOT.equals(path);
        if (!register && !system.equals(pathId)) {
            reject(response, HttpServletResponse.SC_FORBIDDEN,
                    "Token of '%s' cannot modify '%s'".formatted(system, pathId == null ? path : pathId));
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

    private static boolean isSystemsPath(String path) {
        return SYSTEMS_ROOT.equals(path) || path.startsWith(SYSTEMS_PREFIX);
    }
}

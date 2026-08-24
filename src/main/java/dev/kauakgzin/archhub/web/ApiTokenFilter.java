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
 * Guards write operations under {@code /api/v1/systems} with a shared
 * secret ({@code X-Hub-Token}) when {@code archhub.security.token} is
 * configured. Reads (GET) always stay open so the dashboard keeps working
 * without credentials. Disabled entirely when no token is configured.
 *
 * <p>Registered explicitly as a {@link org.springframework.boot.web.servlet.FilterRegistrationBean}
 * in {@code HubConfig} (not {@code @Component}) so it isn't auto-picked up by
 * {@code @WebMvcTest} slices that don't load the full configuration.
 */
public class ApiTokenFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Hub-Token";
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
        if (properties.token().equals(provided)) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", Instant.now().toString(),
                "message", "Missing or invalid " + HEADER + " header"
        ));
    }

    private boolean requiresToken(HttpServletRequest request) {
        return properties.enabled()
                && request.getRequestURI().startsWith("/api/v1/systems")
                && PROTECTED_METHODS.contains(request.getMethod());
    }
}

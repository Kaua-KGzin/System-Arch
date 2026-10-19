package dev.kauakgzin.archhub.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kauakgzin.archhub.config.HubSecurityProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ApiTokenFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void allowsRequestsWhenNoTokenConfigured() throws Exception {
        ApiTokenFilter filter = new ApiTokenFilter(new HubSecurityProperties(null), objectMapper);
        MockHttpServletRequest request = postTo("/api/v1/systems");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void alwaysAllowsGetRequestsEvenWithTokenConfigured() throws Exception {
        ApiTokenFilter filter = new ApiTokenFilter(new HubSecurityProperties("secret"), objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/systems");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsWriteRequestWithoutTokenWhenConfigured() throws Exception {
        ApiTokenFilter filter = new ApiTokenFilter(new HubSecurityProperties("secret"), objectMapper);
        MockHttpServletRequest request = postTo("/api/v1/systems");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsWriteRequestWithCorrectToken() throws Exception {
        ApiTokenFilter filter = new ApiTokenFilter(new HubSecurityProperties("secret"), objectMapper);
        MockHttpServletRequest request = postTo("/api/v1/systems");
        request.addHeader("X-Hub-Token", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void systemTokenWritesOnlyItsOwnId() throws Exception {
        HubSecurityProperties properties = new HubSecurityProperties(null, "archmap=map-secret, system-pvd=pvd-secret");
        ApiTokenFilter filter = new ApiTokenFilter(properties, objectMapper);

        MockHttpServletRequest own = postTo("/api/v1/systems/archmap/architecture");
        own.addHeader("X-Hub-Token", "map-secret");
        FilterChain ownChain = mock(FilterChain.class);
        filter.doFilter(own, new MockHttpServletResponse(), ownChain);
        verify(ownChain).doFilter(org.mockito.ArgumentMatchers.eq(own), org.mockito.ArgumentMatchers.any());

        MockHttpServletRequest other = postTo("/api/v1/systems/system-pvd/heartbeat");
        other.addHeader("X-Hub-Token", "map-secret");
        MockHttpServletResponse otherResponse = new MockHttpServletResponse();
        FilterChain otherChain = mock(FilterChain.class);
        filter.doFilter(other, otherResponse, otherChain);
        assertThat(otherResponse.getStatus()).isEqualTo(403);
        verify(otherChain, never()).doFilter(other, otherResponse);
    }

    @Test
    void systemTokenOnRegisterExposesAuthenticatedId() throws Exception {
        ApiTokenFilter filter = new ApiTokenFilter(new HubSecurityProperties("admin", "archmap=map-secret"), objectMapper);
        MockHttpServletRequest request = postTo("/api/v1/systems");
        request.addHeader("X-Hub-Token", "map-secret");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(request.getAttribute(ApiTokenFilter.AUTHENTICATED_SYSTEM)).isEqualTo("archmap");
    }

    @Test
    void pathSystemIdExtraction() {
        assertThat(ApiTokenFilter.pathSystemId("/api/v1/systems")).isNull();
        assertThat(ApiTokenFilter.pathSystemId("/api/v1/systems/")).isNull();
        assertThat(ApiTokenFilter.pathSystemId("/api/v1/systems/archmap")).isEqualTo("archmap");
        assertThat(ApiTokenFilter.pathSystemId("/api/v1/systems/archmap/heartbeat")).isEqualTo("archmap");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "/api/v1/systems;v=1/system-pvd/heartbeat",
            "/api/v1;x/systems/system-pvd",
            "/api;x/v1/systems/system-pvd/heartbeat"})
    void matrixParametersCannotBypassTheTokenCheck(String uri) throws Exception {
        ApiTokenFilter filter = new ApiTokenFilter(new HubSecurityProperties("admin", "archmap=map-secret"), objectMapper);
        for (String token : new String[]{null, "map-secret"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("DELETE", uri);
            if (token != null) {
                request.addHeader("X-Hub-Token", token);
            }
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(400);
            verify(chain, never()).doFilter(request, response);
        }
    }

    @Test
    void systemTokenCannotWriteOutsideItsOwnId() throws Exception {
        ApiTokenFilter filter = new ApiTokenFilter(new HubSecurityProperties(null, "archmap=map-secret"), objectMapper);
        // "//" collapses to an id-less path: not register, not archmap's own path -> fail closed.
        for (String uri : new String[]{"/api/v1/systems//system-pvd/heartbeat", "/api/v1/systems/%73ystem-pvd/heartbeat"}) {
            MockHttpServletRequest request = postTo(uri);
            request.addHeader("X-Hub-Token", "map-secret");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, mock(FilterChain.class));

            assertThat(response.getStatus()).as(uri).isEqualTo(403);
        }
    }

    @Test
    void blankSystemTokenIsIgnoredAndEmptyHeaderNeverAuthenticates() throws Exception {
        HubSecurityProperties properties = new HubSecurityProperties(null, "archmap= , system-pvd=pvd-secret");
        assertThat(properties.parsedSystemTokens()).containsOnlyKeys("system-pvd");
        assertThat(properties.systemFor("")).isNull();

        ApiTokenFilter filter = new ApiTokenFilter(properties, objectMapper);
        MockHttpServletRequest request = postTo("/api/v1/systems/archmap/heartbeat");
        request.addHeader("X-Hub-Token", "");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    private MockHttpServletRequest postTo(String uri) {
        return new MockHttpServletRequest("POST", uri);
    }
}

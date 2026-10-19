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

    private MockHttpServletRequest postTo(String uri) {
        return new MockHttpServletRequest("POST", uri);
    }
}

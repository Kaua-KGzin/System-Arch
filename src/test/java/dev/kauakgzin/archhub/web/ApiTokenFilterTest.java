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

    private MockHttpServletRequest postTo(String uri) {
        return new MockHttpServletRequest("POST", uri);
    }
}

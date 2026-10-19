package dev.kauakgzin.archhub.service;

import dev.kauakgzin.archhub.web.dto.RegisterSystemRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SelfRegistrationTest {

    private final RegistrationService registrationService = mock(RegistrationService.class);

    @Test
    void registersHubAsArchHubOnTheListeningPort() {
        MockEnvironment env = new MockEnvironment().withProperty("local.server.port", "9090");

        new SelfRegistration(registrationService, env, true, "").onApplicationEvent(null);

        ArgumentCaptor<RegisterSystemRequest> captor = ArgumentCaptor.forClass(RegisterSystemRequest.class);
        verify(registrationService).register(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("arch-hub");
        assertThat(captor.getValue().healthCheckUrl()).isEqualTo("http://localhost:9090/actuator/health");
    }

    @Test
    void usesPublicUrlWhenConfigured() {
        MockEnvironment env = new MockEnvironment().withProperty("local.server.port", "8080");

        new SelfRegistration(registrationService, env, true, "http://hub.lan:8080/").onApplicationEvent(null);

        ArgumentCaptor<RegisterSystemRequest> captor = ArgumentCaptor.forClass(RegisterSystemRequest.class);
        verify(registrationService).register(captor.capture());
        assertThat(captor.getValue().healthCheckUrl()).isEqualTo("http://hub.lan:8080/actuator/health");
    }

    @Test
    void skipsWithoutWebServerOrWhenDisabled() {
        new SelfRegistration(registrationService, new MockEnvironment(), true, "").onApplicationEvent(null);
        new SelfRegistration(registrationService,
                new MockEnvironment().withProperty("local.server.port", "8080"), false, "").onApplicationEvent(null);

        verify(registrationService, never()).register(any());
    }
}

package dev.kauakgzin.archhub.service;

import dev.kauakgzin.archhub.config.HubProperties;
import dev.kauakgzin.archhub.exception.SystemNotFoundException;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import dev.kauakgzin.archhub.web.dto.RegisterSystemRequest;
import dev.kauakgzin.archhub.web.dto.SystemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationServiceTest {

    private final Instant fixedNow = Instant.parse("2026-01-01T12:00:00Z");
    private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
    private final SystemRegistry registry = new SystemRegistry();
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        HubProperties properties = new HubProperties(15000, Duration.ofSeconds(45), Duration.ofSeconds(3));
        service = new RegistrationService(registry, properties, clock);
    }

    private RegisterSystemRequest requestFor(String id) {
        return new RegisterSystemRequest(id, "System " + id, "http://localhost:9000/",
                "http://localhost:9000/health", "desc", List.of("java"));
    }

    @Test
    void registerNewSystemMarksItUpImmediately() {
        SystemResponse response = service.register(requestFor("system-pvd"));

        assertThat(response.id()).isEqualTo("system-pvd");
        assertThat(response.baseUrl()).isEqualTo("http://localhost:9000");
        assertThat(response.status().name()).isEqualTo("UP");
        assertThat(response.registeredAt()).isEqualTo(fixedNow);
    }

    @Test
    void reRegisteringKeepsOriginalRegisteredAt() {
        service.register(requestFor("archmap"));
        Clock laterClock = Clock.fixed(fixedNow.plusSeconds(3600), ZoneOffset.UTC);
        RegistrationService laterService = new RegistrationService(registry,
                new HubProperties(0, null, null), laterClock);

        SystemResponse response = laterService.register(requestFor("archmap"));

        assertThat(response.registeredAt()).isEqualTo(fixedNow);
    }

    @Test
    void heartbeatOnUnknownSystemThrows() {
        assertThatThrownBy(() -> service.heartbeat("does-not-exist"))
                .isInstanceOf(SystemNotFoundException.class);
    }

    @Test
    void deregisterRemovesSystemFromList() {
        service.register(requestFor("simple-arch"));

        service.deregister("simple-arch");

        assertThat(service.list()).isEmpty();
    }

    @Test
    void listIsSortedById() {
        service.register(requestFor("simple-arch"));
        service.register(requestFor("archmap"));

        assertThat(service.list()).extracting(SystemResponse::id)
                .containsExactly("archmap", "simple-arch");
    }
}

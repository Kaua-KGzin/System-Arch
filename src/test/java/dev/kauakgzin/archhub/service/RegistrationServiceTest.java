package dev.kauakgzin.archhub.service;

import dev.kauakgzin.archhub.config.HubProperties;
import dev.kauakgzin.archhub.domain.EventType;
import dev.kauakgzin.archhub.domain.SystemStatus;
import dev.kauakgzin.archhub.exception.SystemNotFoundException;
import dev.kauakgzin.archhub.repository.EventLog;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import dev.kauakgzin.archhub.web.dto.ConnectionResponse;
import dev.kauakgzin.archhub.web.dto.RegisterSystemRequest;
import dev.kauakgzin.archhub.web.dto.StatsResponse;
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
    private final EventLog eventLog = new EventLog();
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        HubProperties properties = new HubProperties(15000, Duration.ofSeconds(45), Duration.ofSeconds(3));
        service = new RegistrationService(registry, properties, clock, eventLog);
    }

    private RegisterSystemRequest requestFor(String id) {
        return requestFor(id, List.of("java"), List.of());
    }

    private RegisterSystemRequest requestFor(String id, List<String> tags, List<String> connectsTo) {
        return new RegisterSystemRequest(id, "System " + id, "http://localhost:9000/",
                "http://localhost:9000/health", "desc", tags, connectsTo);
    }

    @Test
    void registerNewSystemMarksItUpImmediately() {
        SystemResponse response = service.register(requestFor("system-pvd"));

        assertThat(response.id()).isEqualTo("system-pvd");
        assertThat(response.baseUrl()).isEqualTo("http://localhost:9000");
        assertThat(response.status()).isEqualTo(SystemStatus.UP);
        assertThat(response.registeredAt()).isEqualTo(fixedNow);
    }

    @Test
    void registerPublishesRegisteredEvent() {
        service.register(requestFor("system-pvd"));

        assertThat(eventLog.recent(10))
                .extracting(e -> e.type())
                .containsExactly(EventType.REGISTERED);
    }

    @Test
    void reRegisteringKeepsOriginalRegisteredAt() {
        service.register(requestFor("archmap"));
        Clock laterClock = Clock.fixed(fixedNow.plusSeconds(3600), ZoneOffset.UTC);
        RegistrationService laterService = new RegistrationService(registry,
                new HubProperties(0, null, null), laterClock, eventLog);

        SystemResponse response = laterService.register(requestFor("archmap"));

        assertThat(response.registeredAt()).isEqualTo(fixedNow);
    }

    @Test
    void heartbeatOnUnknownSystemThrows() {
        assertThatThrownBy(() -> service.heartbeat("does-not-exist"))
                .isInstanceOf(SystemNotFoundException.class);
    }

    @Test
    void deregisterRemovesSystemFromListAndPublishesEvent() {
        service.register(requestFor("simple-arch"));

        service.deregister("simple-arch");

        assertThat(service.list()).isEmpty();
        assertThat(eventLog.recent(10)).extracting(e -> e.type())
                .containsExactly(EventType.DEREGISTERED, EventType.REGISTERED);
    }

    @Test
    void deregisterUnknownSystemThrows() {
        assertThatThrownBy(() -> service.deregister("ghost"))
                .isInstanceOf(SystemNotFoundException.class);
    }

    @Test
    void listIsSortedById() {
        service.register(requestFor("simple-arch"));
        service.register(requestFor("archmap"));

        assertThat(service.list()).extracting(SystemResponse::id)
                .containsExactly("archmap", "simple-arch");
    }

    @Test
    void listFiltersByTag() {
        service.register(requestFor("archmap", List.of("python"), List.of()));
        service.register(requestFor("simple-arch", List.of("node"), List.of()));

        assertThat(service.list("python", null, null)).extracting(SystemResponse::id)
                .containsExactly("archmap");
    }

    @Test
    void listFiltersByQueryAcrossIdNameAndDescription() {
        service.register(requestFor("archmap", List.of(), List.of()));
        service.register(requestFor("simple-arch", List.of(), List.of()));

        assertThat(service.list(null, null, "ARCHmap")).extracting(SystemResponse::id)
                .containsExactly("archmap");
    }

    @Test
    void listFiltersByStatus() {
        service.register(requestFor("archmap"));

        assertThat(service.list(null, SystemStatus.UP, null)).hasSize(1);
        assertThat(service.list(null, SystemStatus.DOWN, null)).isEmpty();
    }

    @Test
    void connectionsExposeEachDeclaredEdgeAndWhetherTargetIsKnown() {
        service.register(requestFor("archmap", List.of(), List.of("system-pvd", "ghost")));
        service.register(requestFor("system-pvd", List.of(), List.of()));

        assertThat(service.connections()).containsExactlyInAnyOrder(
                new ConnectionResponse("archmap", "system-pvd", true),
                new ConnectionResponse("archmap", "ghost", false)
        );
    }

    @Test
    void statsCountsSystemsByStatusAndConnections() {
        service.register(requestFor("archmap", List.of(), List.of("system-pvd")));
        service.register(requestFor("system-pvd", List.of(), List.of()));

        StatsResponse stats = service.stats();

        assertThat(stats.total()).isEqualTo(2);
        assertThat(stats.up()).isEqualTo(2);
        assertThat(stats.down()).isZero();
        assertThat(stats.connections()).isEqualTo(1);
    }
}

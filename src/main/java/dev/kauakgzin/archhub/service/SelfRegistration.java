package dev.kauakgzin.archhub.service;

import dev.kauakgzin.archhub.web.dto.RegisterSystemRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registers the Hub itself as {@code arch-hub}, the node every system of the
 * Arch line points to via {@code connectsTo}. Without it those edges would show
 * as pending forever. Runs after the snapshot restore (ApplicationRunner), so
 * the fresh registration wins, and only when a real web server is listening.
 */
@Component
public class SelfRegistration implements ApplicationListener<ApplicationReadyEvent> {

    public static final String HUB_ID = "arch-hub";

    private final RegistrationService registrationService;
    private final Environment environment;
    private final boolean enabled;
    private final String publicUrl;

    public SelfRegistration(RegistrationService registrationService, Environment environment,
                            @Value("${archhub.self-register.enabled:true}") boolean enabled,
                            @Value("${archhub.self-register.public-url:}") String publicUrl) {
        this.registrationService = registrationService;
        this.environment = environment;
        this.enabled = enabled;
        this.publicUrl = publicUrl;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String port = environment.getProperty("local.server.port");
        if (!enabled || port == null) {
            return;
        }
        String baseUrl = publicUrl.isBlank() ? "http://localhost:" + port : publicUrl;
        registrationService.register(new RegisterSystemRequest(
                HUB_ID,
                "Arch Hub",
                baseUrl,
                baseUrl.replaceAll("/+$", "") + "/actuator/health",
                "Catálogo central e monitoramento da linha Arch",
                List.of("arch-line", "role:platform", "java", "spring-boot"),
                List.of()
        ));
    }
}

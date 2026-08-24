package dev.kauakgzin.archhub.web;

import dev.kauakgzin.archhub.service.RegistrationService;
import dev.kauakgzin.archhub.web.dto.StatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Stats", description = "Resumo agregado do estado do Hub")
public class StatsController {

    private final RegistrationService registrationService;

    public StatsController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    @Operation(summary = "Contagem de sistemas por status e total de conexoes")
    public StatsResponse stats() {
        return registrationService.stats();
    }
}

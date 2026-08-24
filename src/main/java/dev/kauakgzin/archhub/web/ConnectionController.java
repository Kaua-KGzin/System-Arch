package dev.kauakgzin.archhub.web;

import dev.kauakgzin.archhub.service.RegistrationService;
import dev.kauakgzin.archhub.web.dto.ConnectionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/connections")
@Tag(name = "Connections", description = "Grafo de conexoes declaradas entre os sistemas")
public class ConnectionController {

    private final RegistrationService registrationService;

    public ConnectionController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    @Operation(summary = "Lista as conexoes (arestas) entre os sistemas registrados")
    public List<ConnectionResponse> list() {
        return registrationService.connections();
    }
}

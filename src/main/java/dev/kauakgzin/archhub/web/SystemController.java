package dev.kauakgzin.archhub.web;

import dev.kauakgzin.archhub.domain.SystemStatus;
import dev.kauakgzin.archhub.service.RegistrationService;
import dev.kauakgzin.archhub.web.dto.RegisterSystemRequest;
import dev.kauakgzin.archhub.web.dto.SystemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/systems")
@Tag(name = "Systems", description = "Registro e consulta dos sistemas conectados ao Hub")
public class SystemController {

    private final RegistrationService registrationService;

    public SystemController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @Operation(summary = "Registra (ou atualiza) um sistema")
    public ResponseEntity<SystemResponse> register(@Valid @RequestBody RegisterSystemRequest request) {
        SystemResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lista os sistemas conectados, com filtros opcionais")
    public List<SystemResponse> list(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) SystemStatus status,
            @RequestParam(required = false) String q) {
        return registrationService.list(tag, status, q);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um sistema")
    public SystemResponse get(@PathVariable String id) {
        return registrationService.get(id);
    }

    @PostMapping("/{id}/heartbeat")
    @Operation(summary = "Heartbeat manual, para sistemas sem health check HTTP acessível")
    public SystemResponse heartbeat(@PathVariable String id) {
        return registrationService.heartbeat(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um sistema do catálogo")
    public ResponseEntity<Void> deregister(@PathVariable String id) {
        registrationService.deregister(id);
        return ResponseEntity.noContent().build();
    }
}

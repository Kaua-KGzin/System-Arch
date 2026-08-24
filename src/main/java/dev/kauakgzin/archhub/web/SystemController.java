package dev.kauakgzin.archhub.web;

import dev.kauakgzin.archhub.service.RegistrationService;
import dev.kauakgzin.archhub.web.dto.RegisterSystemRequest;
import dev.kauakgzin.archhub.web.dto.SystemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/systems")
public class SystemController {

    private final RegistrationService registrationService;

    public SystemController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<SystemResponse> register(@Valid @RequestBody RegisterSystemRequest request) {
        SystemResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<SystemResponse> list() {
        return registrationService.list();
    }

    @GetMapping("/{id}")
    public SystemResponse get(@PathVariable String id) {
        return registrationService.get(id);
    }

    @PostMapping("/{id}/heartbeat")
    public SystemResponse heartbeat(@PathVariable String id) {
        return registrationService.heartbeat(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deregister(@PathVariable String id) {
        registrationService.deregister(id);
        return ResponseEntity.noContent().build();
    }
}

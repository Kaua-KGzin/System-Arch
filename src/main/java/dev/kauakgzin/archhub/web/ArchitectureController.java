package dev.kauakgzin.archhub.web;

import dev.kauakgzin.archhub.domain.ArchitectureReport;
import dev.kauakgzin.archhub.service.ArchitectureService;
import dev.kauakgzin.archhub.web.dto.ArchitectureEntry;
import dev.kauakgzin.archhub.web.dto.ArchitectureReportRequest;
import dev.kauakgzin.archhub.web.dto.ArchitectureSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@Tag(name = "Architecture", description = "Score de arquitetura publicado pelo ArchMAP (normalmente via CI)")
public class ArchitectureController {

    private static final String SLUG = "^[a-z0-9](?:[a-z0-9-]{0,62})$";

    private final ArchitectureService architectureService;

    public ArchitectureController(ArchitectureService architectureService) {
        this.architectureService = architectureService;
    }

    @PostMapping("/api/v1/systems/{id}/architecture")
    @Operation(summary = "Publica a análise de arquitetura de um sistema (registrado ou não)")
    public ResponseEntity<ArchitectureSummary> report(@PathVariable @Pattern(regexp = SLUG) String id,
                                                      @Valid @RequestBody ArchitectureReportRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(architectureService.report(id, request));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).build();
        }
    }

    @GetMapping("/api/v1/systems/{id}/architecture")
    @Operation(summary = "Histórico de análises de um sistema (mais recente primeiro)")
    public List<ArchitectureReport> history(@PathVariable String id) {
        return architectureService.history(id);
    }

    @GetMapping("/api/v1/architecture")
    @Operation(summary = "Último score de cada sistema da linha")
    public List<ArchitectureEntry> all() {
        return architectureService.all();
    }
}

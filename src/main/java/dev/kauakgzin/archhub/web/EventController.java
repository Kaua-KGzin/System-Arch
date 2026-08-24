package dev.kauakgzin.archhub.web;

import dev.kauakgzin.archhub.repository.EventLog;
import dev.kauakgzin.archhub.web.dto.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Feed de atividade: registros, remocoes e mudancas de status")
public class EventController {

    private final EventLog eventLog;

    public EventController(EventLog eventLog) {
        this.eventLog = eventLog;
    }

    @GetMapping
    @Operation(summary = "Eventos mais recentes, do mais novo para o mais antigo")
    public List<EventResponse> recent(@RequestParam(defaultValue = "50") int limit) {
        int bounded = Math.max(1, Math.min(limit, 200));
        return eventLog.recent(bounded).stream().map(EventResponse::of).toList();
    }
}

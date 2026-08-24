package dev.kauakgzin.archhub.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kauakgzin.archhub.domain.SystemStatus;
import dev.kauakgzin.archhub.exception.SystemNotFoundException;
import dev.kauakgzin.archhub.service.RegistrationService;
import dev.kauakgzin.archhub.web.dto.RegisterSystemRequest;
import dev.kauakgzin.archhub.web.dto.SystemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SystemController.class)
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    private SystemResponse sampleResponse(String id) {
        return new SystemResponse(id, "System " + id, "http://localhost:9000", null,
                "desc", List.of("java"), List.of(), SystemStatus.UP, Instant.now(), Instant.now(), null);
    }

    @Test
    void registerReturns201() throws Exception {
        RegisterSystemRequest request = new RegisterSystemRequest(
                "system-pvd", "System PVD", "http://localhost:9000", null, "desc", List.of(), List.of());
        when(registrationService.register(any())).thenReturn(sampleResponse("system-pvd"));

        mockMvc.perform(post("/api/v1/systems")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("system-pvd"));
    }

    @Test
    void registerWithBlankNameReturns400() throws Exception {
        RegisterSystemRequest request = new RegisterSystemRequest(
                "system-pvd", "", "http://localhost:9000", null, null, null, null);

        mockMvc.perform(post("/api/v1/systems")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void listReturnsAllSystems() throws Exception {
        when(registrationService.list(isNull(), isNull(), isNull()))
                .thenReturn(List.of(sampleResponse("archmap"), sampleResponse("simple-arch")));

        mockMvc.perform(get("/api/v1/systems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listPassesFiltersThrough() throws Exception {
        when(registrationService.list(eq("python"), eq(SystemStatus.UP), isNull()))
                .thenReturn(List.of(sampleResponse("archmap")));

        mockMvc.perform(get("/api/v1/systems").param("tag", "python").param("status", "UP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getUnknownSystemReturns404() throws Exception {
        when(registrationService.get(eq("ghost"))).thenThrow(new SystemNotFoundException("ghost"));

        mockMvc.perform(get("/api/v1/systems/ghost"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deregisterReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/systems/system-pvd"))
                .andExpect(status().isNoContent());
    }
}

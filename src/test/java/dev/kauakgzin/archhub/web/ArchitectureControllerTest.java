package dev.kauakgzin.archhub.web;

import dev.kauakgzin.archhub.repository.ArchitectureStore;
import dev.kauakgzin.archhub.repository.EventLog;
import dev.kauakgzin.archhub.repository.SystemRegistry;
import dev.kauakgzin.archhub.service.ArchitectureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArchitectureController.class)
@Import({ArchitectureService.class, ArchitectureStore.class, SystemRegistry.class, EventLog.class,
        ArchitectureControllerTest.FixedClock.class})
class ArchitectureControllerTest {

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventLog eventLog;

    private static String body(int score) {
        return """
                {"score": %d, "grade": "B", "files": 120, "cycles": 1, "findings": 4,
                 "commit": "abc123", "ref": "main", "toolVersion": "1.5.0"}
                """.formatted(score);
    }

    @Test
    void reportsAccumulateWithDeltaAndTrend() throws Exception {
        mockMvc.perform(post("/api/v1/systems/archmap/architecture").contentType("application/json").content(body(70)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.delta").doesNotExist());
        mockMvc.perform(post("/api/v1/systems/archmap/architecture").contentType("application/json").content(body(82)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.latest.score").value(82))
                .andExpect(jsonPath("$.delta").value(12))
                .andExpect(jsonPath("$.trend[0]").value(70))
                .andExpect(jsonPath("$.trend[1]").value(82));

        mockMvc.perform(get("/api/v1/architecture"))
                .andExpect(jsonPath("$[0].systemId").value("archmap"))
                .andExpect(jsonPath("$[0].registered").value(false));
        mockMvc.perform(get("/api/v1/systems/archmap/architecture"))
                .andExpect(jsonPath("$.length()").value(2));
        org.assertj.core.api.Assertions.assertThat(eventLog.recent(1).get(0).message()).contains("82/100").contains("▲12");
    }

    @Test
    void rejectsOutOfRangeScoreAndBadId() throws Exception {
        mockMvc.perform(post("/api/v1/systems/archmap/architecture").contentType("application/json").content(body(101)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/systems/Not_A_Slug/architecture").contentType("application/json").content(body(50)))
                .andExpect(status().isBadRequest());
    }
}

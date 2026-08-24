package dev.kauakgzin.archhub.web;

import dev.kauakgzin.archhub.service.NetworkInfoService;
import dev.kauakgzin.archhub.service.QrCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NetworkController.class)
class NetworkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NetworkInfoService networkInfoService;

    @MockBean
    private QrCodeService qrCodeService;

    @Test
    void networkListsUrlsWithFirstAsPrimary() throws Exception {
        when(networkInfoService.urls()).thenReturn(List.of("http://192.168.1.20:8080", "http://10.0.0.5:8080"));

        mockMvc.perform(get("/api/v1/network"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryUrl").value("http://192.168.1.20:8080"))
                .andExpect(jsonPath("$.urls.length()").value(2));
    }

    @Test
    void networkWithNoLanAddressReturnsNullPrimary() throws Exception {
        when(networkInfoService.urls()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/network"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryUrl").doesNotExist());
    }

    @Test
    void qrReturnsSvgWhenLanAddressAvailable() throws Exception {
        when(networkInfoService.primaryUrl()).thenReturn(Optional.of("http://192.168.1.20:8080"));
        when(qrCodeService.toSvg("http://192.168.1.20:8080")).thenReturn("<svg>stub</svg>");

        mockMvc.perform(get("/api/v1/network/qr.svg"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(content().string("<svg>stub</svg>"));
    }

    @Test
    void qrReturnsNoContentWhenNoLanAddress() throws Exception {
        when(networkInfoService.primaryUrl()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/network/qr.svg"))
                .andExpect(status().isNoContent());
    }
}

package dev.kauakgzin.archhub.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeServiceTest {

    private final QrCodeService service = new QrCodeService();

    @Test
    void producesWellFormedSvgWithDarkModules() {
        String svg = service.toSvg("http://192.168.1.20:8080");

        assertThat(svg).startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\"");
        assertThat(svg).contains("viewBox=");
        assertThat(svg).endsWith("</svg>");
        assertThat(svg).contains("fill=\"#0f1115\"");
    }

    @Test
    void isDeterministicForTheSameContent() {
        assertThat(service.toSvg("http://192.168.1.20:8080"))
                .isEqualTo(service.toSvg("http://192.168.1.20:8080"));
    }

    @Test
    void differsForDifferentContent() {
        assertThat(service.toSvg("http://192.168.1.20:8080"))
                .isNotEqualTo(service.toSvg("http://192.168.1.21:8080"));
    }
}

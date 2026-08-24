package dev.kauakgzin.archhub.web;

import dev.kauakgzin.archhub.service.NetworkInfoService;
import dev.kauakgzin.archhub.service.QrCodeService;
import dev.kauakgzin.archhub.web.dto.NetworkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/network")
@Tag(name = "Network", description = "Enderecos de rede local do Hub, para abrir o dashboard a partir do celular")
public class NetworkController {

    private final NetworkInfoService networkInfoService;
    private final QrCodeService qrCodeService;

    public NetworkController(NetworkInfoService networkInfoService, QrCodeService qrCodeService) {
        this.networkInfoService = networkInfoService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping
    @Operation(summary = "URLs pelas quais o Hub e alcancavel na rede local")
    public NetworkResponse network() {
        var urls = networkInfoService.urls();
        return new NetworkResponse(urls.isEmpty() ? null : urls.get(0), urls);
    }

    @GetMapping(value = "/qr.svg", produces = "image/svg+xml")
    @Operation(summary = "QR code (SVG) apontando para o Hub na rede local, para escanear com o celular")
    public ResponseEntity<String> qr() {
        return networkInfoService.primaryUrl()
                .map(url -> ResponseEntity.ok(qrCodeService.toSvg(url)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}

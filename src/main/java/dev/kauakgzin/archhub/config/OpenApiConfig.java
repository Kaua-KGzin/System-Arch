package dev.kauakgzin.archhub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI archHubOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Arch Hub API")
                .description("Catalogo central de conexao entre os sistemas Arch (System-PVD, ArchMAP, SIMPLE-ArCh e outros).")
                .version("v1"));
    }
}

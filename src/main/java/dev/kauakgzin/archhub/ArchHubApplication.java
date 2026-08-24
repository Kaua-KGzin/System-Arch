package dev.kauakgzin.archhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArchHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchHubApplication.class, args);
    }
}

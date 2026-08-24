package dev.kauakgzin.archhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class ArchHubApplication {

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--version")) {
            // Packaged builds (jpackage) can be smoke-tested with this flag without
            // booting the full Spring context. Implementation-Version is stamped into
            // the manifest by spring-boot-maven-plugin's repackage goal.
            String version = ArchHubApplication.class.getPackage().getImplementationVersion();
            System.out.println("arch-hub " + (version != null ? version : "dev"));
            return;
        }
        SpringApplication.run(ArchHubApplication.class, args);
    }
}

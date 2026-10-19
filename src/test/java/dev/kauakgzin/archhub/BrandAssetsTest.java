package dev.kauakgzin.archhub;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** The dashboard serves a copy of the Arch line brand kit; it must not drift from brand/. */
class BrandAssetsTest {

    @ParameterizedTest
    @ValueSource(strings = {"arch-mark.svg", "arch-line.css"})
    void staticCopyMatchesBrandSource(String file) throws IOException {
        assertThat(Files.readString(Path.of("src/main/resources/static/brand", file)))
                .isEqualTo(Files.readString(Path.of("brand", file)));
    }
}

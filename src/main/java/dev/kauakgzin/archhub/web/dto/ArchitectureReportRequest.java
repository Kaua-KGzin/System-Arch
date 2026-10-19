package dev.kauakgzin.archhub.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/v1/systems/{id}/architecture}, sent by {@code archmap hub-report}. */
public record ArchitectureReportRequest(
        @Min(0) @Max(100) int score,
        @Size(max = 4) String grade,
        @Min(0) int files,
        @Min(0) int cycles,
        @Min(0) int findings,
        @Size(max = 64) String commit,
        @Size(max = 200) String ref,
        @Size(max = 32) String toolVersion,
        @Size(max = 500) @Pattern(regexp = "^https?://.*", message = "reportUrl must be http(s)") String reportUrl
) {
}

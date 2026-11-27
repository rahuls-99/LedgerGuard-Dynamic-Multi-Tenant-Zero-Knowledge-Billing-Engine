package com.ledgerguard.analytics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AnalyticsStaticTimeCheckTest {

    @Test
    void noForbiddenTimeTokensInAnalyticsOrSeedPackages() throws IOException {
        List<String> forbidden = List.of(
                "LocalDate.now(",
                "LocalDateTime.now(",
                "Instant.now(",
                "ZoneId.systemDefault(",
                "WeekFields",
                "Timestamp.valueOf",
                "new Date("
        );

        List<Path> roots = List.of(
                Path.of("src/main/java/com/ledgerguard/analytics"),
                Path.of("src/main/java/com/ledgerguard/analytics/seed")
        );

        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String content = Files.readString(file);
                    for (String token : forbidden) {
                        assertFalse(content.contains(token),
                                "Forbidden token '" + token + "' found in " + file);
                    }
                }
            }
        }
    }
}
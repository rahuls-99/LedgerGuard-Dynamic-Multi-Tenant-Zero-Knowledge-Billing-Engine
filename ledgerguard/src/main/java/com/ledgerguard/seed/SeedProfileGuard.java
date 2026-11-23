package com.ledgerguard.seed;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class SeedProfileGuard {

    private static final Set<String> ALLOWED_PROFILES = Set.of("demo", "e2e", "dev");

    private final Environment environment;

    public SeedProfileGuard(Environment environment) {
        this.environment = environment;
    }

    public boolean isSeedAllowed() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(ALLOWED_PROFILES::contains);
    }

    public void assertSeedAllowed(boolean seedRequested) {
        if (seedRequested && !isSeedAllowed()) {
            throw new IllegalStateException(
                    "--seed-analytics is forbidden outside profiles [demo, e2e, dev]. Active: "
                            + Arrays.toString(environment.getActiveProfiles()));
        }
    }
}
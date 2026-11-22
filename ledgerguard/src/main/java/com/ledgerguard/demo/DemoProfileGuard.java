package com.ledgerguard.demo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import java.time.Duration;
@Component
public class DemoProfileGuard {
    private static final Logger log = LoggerFactory.getLogger(DemoProfileGuard.class);
    private final Environment environment;
    private final Duration reaperStuckAfter;
    public DemoProfileGuard(
            Environment environment,
            @Value("${ledger.reaper.stuck-after:PT2M}") Duration reaperStuckAfter) {
        this.environment = environment;
        this.reaperStuckAfter = reaperStuckAfter;
    }
    @PostConstruct
    public void validateEnvironment() {
        boolean isDemo = environment.acceptsProfiles(Profiles.of("demo"));
        boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));
        if (isDemo && isProd) {
            throw new IllegalStateException(
                "FATAL SECURITY ERROR: 'demo' and 'prod' profiles cannot be active simultaneously!"
            );
        }
        if (reaperStuckAfter.compareTo(Duration.ofSeconds(30)) < 0) {
            throw new IllegalStateException(
                "FATAL CONFIG ERROR: ledger.reaper.stuck-after (" + reaperStuckAfter + ") must be at least 30 seconds!"
            );
        }
        if (isDemo) {
            log.warn("\n====================================================================\n" +
                     "  WARNING: LEDGERGUARD IS RUNNING IN 'DEMO' PROFILE MODE!           \n" +
                     "  Destructive failure/crash injection hooks are ENABLED.            \n" +
                     "  DO NOT USE THIS CONFIGURATION IN PRODUCTION.                      \n" +
                     "====================================================================");
        }
    }
}

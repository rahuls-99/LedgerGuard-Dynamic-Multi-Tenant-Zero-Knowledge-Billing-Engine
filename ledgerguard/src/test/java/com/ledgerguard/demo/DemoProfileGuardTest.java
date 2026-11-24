package com.ledgerguard.demo;
import com.ledgerguard.ledger.posting.FailureInjector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class DemoProfileGuardTest {
    @Test
    void defaultProfile_hasNoDemoFailureInjectorBean() {
        new ApplicationContextRunner()
                .withUserConfiguration(DemoFailureInjector.class)
                .run(context -> assertThat(context).doesNotHaveBean(DemoFailureInjector.class));
    }
    @Test
    void demoAndProdTogether_failsStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("demo", "prod");
        DemoProfileGuard guard = new DemoProfileGuard(env, Duration.ofSeconds(45));
        assertThatThrownBy(guard::validateEnvironment)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demo")
                .hasMessageContaining("prod");
    }
    @Test
    void reaperStuckAfterBelow30s_failsStartup() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("demo");
        DemoProfileGuard guard = new DemoProfileGuard(env, Duration.ofSeconds(20));
        assertThatThrownBy(guard::validateEnvironment)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("30 seconds");
    }
    @Test
    void afterDebitFail_consumesFileAndThrows_thenNextCallIsNormal(@TempDir Path tempDir) throws Exception {
        Path armFile = tempDir.resolve("failure");
        Files.writeString(armFile, "AFTER_DEBIT:FAIL");
        DemoFailureInjector injector = new DemoFailureInjector(armFile.toString());
        assertThatThrownBy(() -> injector.checkpoint(FailureInjector.Point.AFTER_DEBIT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AFTER_DEBIT");
        assertThat(Files.exists(armFile)).isFalse();
        injector.checkpoint(FailureInjector.Point.AFTER_DEBIT);
    }
}

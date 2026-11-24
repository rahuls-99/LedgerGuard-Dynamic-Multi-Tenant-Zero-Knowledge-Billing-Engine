package com.ledgerguard.ledger;

import com.ledgerguard.ledger.posting.FailureInjector;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class TestFailureInjector implements FailureInjector {

    public enum Mode { NONE, DELAY, FAIL, FAIL_ONCE }
    public record InjectionRule(Mode mode, Duration delay) {}

    private final Map<String, InjectionRule> rules = new ConcurrentHashMap<>();

    public void setDelay(String point, Duration duration) {
        if (point != null) rules.put(point, new InjectionRule(Mode.DELAY, duration != null ? duration : Duration.ZERO));
    }

    public void setDelay(Point point, Duration duration) {
        if (point != null) setDelay(point.name(), duration);
    }

    public void setFail(String point, boolean enabled) {
        if (point == null) return;
        if (enabled) rules.put(point, new InjectionRule(Mode.FAIL, Duration.ZERO)); else rules.remove(point);
    }

    public void failOnceAt(Point point) {
        if (point != null) rules.put(point.name(), new InjectionRule(Mode.FAIL_ONCE, Duration.ZERO));
    }

    public void failOnceAt(String point) {
        if (point != null) rules.put(point, new InjectionRule(Mode.FAIL_ONCE, Duration.ZERO));
    }

    public void clear(String point) {
        if (point != null) rules.remove(point);
    }

    public void clear() {
        rules.clear();
    }

    @Override
    public void checkpoint(Point point) {
        if (point != null) checkpoint(point.name());
    }

    public void checkpoint(String key) {
        if (key == null) return;
        InjectionRule rule = rules.get(key);
        if (rule == null || rule.mode() == Mode.NONE) return;
        if (rule.mode() == Mode.DELAY && rule.delay() != null && !rule.delay().isZero()) {
            try {
                Thread.sleep(Math.max(1L, rule.delay().toMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        } else if (rule.mode() == Mode.FAIL) {
            throw new IllegalStateException("Injected failure at " + key);
        } else if (rule.mode() == Mode.FAIL_ONCE) {
            rules.remove(key);
            throw new IllegalStateException("Injected one-time failure at " + key);
        }
    }
}

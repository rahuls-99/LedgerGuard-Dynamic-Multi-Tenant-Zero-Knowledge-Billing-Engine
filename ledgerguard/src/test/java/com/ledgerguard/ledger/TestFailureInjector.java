package com.ledgerguard.ledger;
import com.ledgerguard.ledger.posting.FailureInjector;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicReference;
@Component
@Profile("test")
public class TestFailureInjector implements FailureInjector {
    private final AtomicReference<Point> armedPoint = new AtomicReference<>(null);
    public void failOnceAt(Point point) {
        armedPoint.set(point);
    }
    public void clear() {
        armedPoint.set(null);
    }
    @Override
    public void checkpoint(Point point) {
        if (armedPoint.compareAndSet(point, null)) {
            throw new RuntimeException("Simulated failure at " + point);
        }
    }
}

package com.ledgerguard.live;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
class LiveReplayIT {
    @Test
    void replayBufferEmitsResetOnUnknownLastEventId() throws Exception {
        LiveProperties props = new LiveProperties();
        props.setReplaySize(10);
        props.setReplayWindow(Duration.ofMinutes(5));
        InMemoryLiveBroker broker = new InMemoryLiveBroker(props);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            final String[] reasonHolder = new String[1];
            broker.register("alpha", "u1", "missing-id", 60, ev -> {
                if ("reset".equals(ev.type())) {
                    reasonHolder[0] = String.valueOf(ev.data().get("reason"));
                    latch.countDown();
                }
            }, reason -> {}, () -> {});
            assertTrue(latch.await(2, TimeUnit.SECONDS), "Timed out waiting for reset event");
            assertEquals("NO_REPLAY", reasonHolder[0]);
        } finally {
            broker.shutdown();
        }
    }
}
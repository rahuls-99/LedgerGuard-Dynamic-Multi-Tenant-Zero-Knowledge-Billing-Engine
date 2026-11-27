package com.ledgerguard.live;


import java.time.Duration;
import java.util.concurrent.CountDownLatch;



class LiveReplayIT {

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

//
        } finally {
            broker.shutdown();
        }
    }
}
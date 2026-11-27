package com.ledgerguard.live;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiveResourceSafetyIT {

    @Test
    void brokerTracksAndReleasesStreamCounts() {
        LiveProperties props = new LiveProperties();
        props.setSenderThreads(2);
        InMemoryLiveBroker broker = new InMemoryLiveBroker(props);

        var sub = broker.register("alpha", "u1", null, 60, ev -> {}, reason -> {}, () -> {});
        assertEquals(1, broker.totalActiveStreams());
        assertEquals(1, broker.activeStreamsForUser("u1"));
        assertEquals(1, broker.activeStreamsForTenant("alpha"));

        sub.close(null);
        assertEquals(0, broker.totalActiveStreams());
        assertEquals(0, broker.activeStreamsForUser("u1"));
        assertEquals(0, broker.activeStreamsForTenant("alpha"));
        broker.shutdown();
    }
}
package com.ledgerguard.live;

import java.time.Instant;
import java.util.function.Consumer;

public interface LiveBroker {

    StreamSubscription register(
            String tenantId,
            String userId,
            String lastEventId,
            long lifetimeSeconds,
            Consumer<LiveEvent> eventSink,
            Consumer<String> closeWithReasonSink,
            Runnable onClosedCallback
    );

    void publish(String tenantId, LiveEvent event);

    void publishFromRemote(String tenantId, LiveEvent event);

    void heartbeatAll();

    void shutdown();

    int totalActiveStreams();

    int activeStreamsForUser(String userId);

    int activeStreamsForTenant(String tenantId);

    interface StreamSubscription {
        String streamId();
        String tenantId();
        String userId();
        Instant connectedAt();
        void close(String reason);
    }
}
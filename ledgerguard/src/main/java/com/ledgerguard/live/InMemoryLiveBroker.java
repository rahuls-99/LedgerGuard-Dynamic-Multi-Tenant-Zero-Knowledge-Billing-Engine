package com.ledgerguard.live;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Component
public class InMemoryLiveBroker implements LiveBroker {

    private static final Logger log = LoggerFactory.getLogger(InMemoryLiveBroker.class);

    private final LiveProperties properties;
    private final ExecutorService senderPool;
    private final Map<String, ConcurrentHashMap<String, ClientStream>> streamsByTenant = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> userCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> tenantCounts = new ConcurrentHashMap<>();
    private final Map<String, Deque<LiveEvent>> replayBuffers = new ConcurrentHashMap<>();
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    public InMemoryLiveBroker(LiveProperties properties) {
        this.properties = properties;
        int threads = Math.max(1, properties.getSenderThreads());
        AtomicInteger threadIdx = new AtomicInteger(1);
        this.senderPool = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "sse-sender-" + threadIdx.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public StreamSubscription register(
            String tenantId,
            String userId,
            String lastEventId,
            long lifetimeSeconds,
            Consumer<LiveEvent> eventSink,
            Consumer<String> closeWithReasonSink,
            Runnable onClosedCallback
    ) {
        String streamId = UUID.randomUUID().toString();
        ClientStream stream = new ClientStream(
                streamId,
                tenantId,
                userId,
                Instant.now(),
                Math.max(1, properties.getQueueSize()),
                eventSink,
                closeWithReasonSink,
                onClosedCallback
        );

        streamsByTenant.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>()).put(streamId, stream);
        userCounts.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
        tenantCounts.computeIfAbsent(tenantId, k -> new AtomicInteger(0)).incrementAndGet();
        totalCount.incrementAndGet();

        if (lastEventId != null && !lastEventId.isBlank()) {
            enqueueReplayOrReset(tenantId, stream, lastEventId);
        }

        return stream;
    }

    private void enqueueReplayOrReset(String tenantId, ClientStream stream, String lastEventId) {
        Deque<LiveEvent> buffer = replayBuffers.get(tenantId);
        if (buffer == null || buffer.isEmpty()) {
            stream.enqueue(LiveEvent.control(null, "reset", Map.of("reason", "NO_REPLAY")));
            return;
        }

        Instant cutoff = Instant.now().minus(properties.getReplayWindow());
        List<LiveEvent> missed = new ArrayList<>();
        boolean found = false;

        synchronized (buffer) {
            for (LiveEvent ev : buffer) {
                if (ev.timestamp().isBefore(cutoff)) {
                    continue;
                }
                if (found) {
                    missed.add(ev);
                } else if (lastEventId.equals(ev.id())) {
                    found = true;
                }
            }
        }

        if (!found) {
            stream.enqueue(LiveEvent.control(null, "reset", Map.of("reason", "REPLAY_GAP")));
            return;
        }

        for (LiveEvent ev : missed) {
            stream.enqueue(ev);
        }
    }

    @Override
    public void publish(String tenantId, LiveEvent event) {
        if (shuttingDown.get() || tenantId == null || event == null) {
            return;
        }
        recordInReplayBuffer(tenantId, event);
        deliverLocal(tenantId, event);
    }

    @Override
    public void publishFromRemote(String tenantId, LiveEvent event) {
        if (shuttingDown.get() || tenantId == null || event == null) {
            return;
        }
        recordInReplayBuffer(tenantId, event);
        deliverLocal(tenantId, event);
    }

    private void deliverLocal(String tenantId, LiveEvent event) {
        Map<String, ClientStream> tenantStreams = streamsByTenant.get(tenantId);
        if (tenantStreams == null || tenantStreams.isEmpty()) {
            return;
        }
        for (ClientStream stream : tenantStreams.values()) {
            stream.enqueue(event);
        }
    }

    private void recordInReplayBuffer(String tenantId, LiveEvent event) {
        int maxSize = Math.max(0, properties.getReplaySize());
        if (maxSize == 0) {
            return;
        }
        Deque<LiveEvent> buffer = replayBuffers.computeIfAbsent(tenantId, k -> new ArrayDeque<>());
        Instant cutoff = Instant.now().minus(properties.getReplayWindow());
        synchronized (buffer) {
            while (!buffer.isEmpty() && buffer.peekFirst().timestamp().isBefore(cutoff)) {
                buffer.pollFirst();
            }
            while (buffer.size() >= maxSize) {
                buffer.pollFirst();
            }
            buffer.addLast(event);
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${live.heartbeat:PT15S}")
    public void heartbeatAll() {
        if (shuttingDown.get()) {
            return;
        }
        LiveEvent hb = LiveEvent.control(null, "heartbeat", Map.of("serverTime", Instant.now().toString()));
        for (Map<String, ClientStream> tenantStreams : streamsByTenant.values()) {
            for (ClientStream stream : tenantStreams.values()) {
                stream.enqueue(hb);
            }
        }
    }

    @Override
    @PreDestroy
    public void shutdown() {
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }
        for (Map<String, ClientStream> tenantStreams : streamsByTenant.values()) {
            for (ClientStream stream : new ArrayList<>(tenantStreams.values())) {
                stream.close("SERVER_SHUTDOWN");
            }
        }
        senderPool.shutdownNow();
    }

    @Override
    public int totalActiveStreams() {
        return Math.max(0, totalCount.get());
    }

    @Override
    public int activeStreamsForUser(String userId) {
        AtomicInteger c = userCounts.get(userId);
        return c == null ? 0 : Math.max(0, c.get());
    }

    @Override
    public int activeStreamsForTenant(String tenantId) {
        AtomicInteger c = tenantCounts.get(tenantId);
        return c == null ? 0 : Math.max(0, c.get());
    }

    private final class ClientStream implements StreamSubscription {
        private final String streamId;
        private final String tenantId;
        private final String userId;
        private final Instant connectedAt;
        private final BlockingQueue<LiveEvent> queue;
        private final Consumer<LiveEvent> eventSink;
        private final Consumer<String> closeWithReasonSink;
        private final Runnable onClosedCallback;

        private final AtomicBoolean draining = new AtomicBoolean(false);
        private final AtomicBoolean closed = new AtomicBoolean(false);

        ClientStream(
                String streamId,
                String tenantId,
                String userId,
                Instant connectedAt,
                int capacity,
                Consumer<LiveEvent> eventSink,
                Consumer<String> closeWithReasonSink,
                Runnable onClosedCallback
        ) {
            this.streamId = streamId;
            this.tenantId = tenantId;
            this.userId = userId;
            this.connectedAt = connectedAt;
            this.queue = new LinkedBlockingQueue<>(capacity);
            this.eventSink = eventSink;
            this.closeWithReasonSink = closeWithReasonSink;
            this.onClosedCallback = onClosedCallback;
        }

        void enqueue(LiveEvent event) {
            if (closed.get()) {
                return;
            }
            if (!queue.offer(event)) {
                log.warn("Slow SSE consumer streamId={} tenantId={} userId={}", streamId, tenantId, userId);
                close("SLOW_CONSUMER");
                return;
            }
            scheduleDrainIfNeeded();
        }

        private void scheduleDrainIfNeeded() {
            if (closed.get()) {
                return;
            }
            if (draining.compareAndSet(false, true)) {
                try {
                    senderPool.execute(this::drainLoop);
                } catch (RuntimeException ex) {
                    draining.set(false);
                    if (!shuttingDown.get()) {
                        close("SENDER_REJECTED");
                    }
                }
            }
        }

        private void drainLoop() {
            try {
                while (!closed.get()) {
                    LiveEvent next = queue.poll();
                    if (next == null) {
                        break;
                    }
                    eventSink.accept(next);
                }
            } catch (RuntimeException ex) {
                log.debug("SSE drain stopped for streamId={}: {}", streamId, ex.getMessage());
                close("WRITE_ERROR");
            } finally {
                draining.set(false);
                if (!closed.get() && !queue.isEmpty()) {
                    scheduleDrainIfNeeded();
                }
            }
        }

        @Override
        public void close(String reason) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            queue.clear();
            Map<String, ClientStream> tenantStreams = streamsByTenant.get(tenantId);
            if (tenantStreams != null) {
                tenantStreams.remove(streamId);
            }
            AtomicInteger uCount = userCounts.get(userId);
            if (uCount != null) {
                uCount.decrementAndGet();
            }
            AtomicInteger tCount = tenantCounts.get(tenantId);
            if (tCount != null) {
                tCount.decrementAndGet();
            }
            totalCount.decrementAndGet();

            try {
                if (reason != null && closeWithReasonSink != null) {
                    closeWithReasonSink.accept(reason);
                }
            } catch (RuntimeException ignored) {
            } finally {
                if (onClosedCallback != null) {
                    onClosedCallback.run();
                }
            }
        }

        @Override
        public String streamId() { return streamId; }

        @Override
        public String tenantId() { return tenantId; }

        @Override
        public String userId() { return userId; }

        @Override
        public Instant connectedAt() { return connectedAt; }
    }
}
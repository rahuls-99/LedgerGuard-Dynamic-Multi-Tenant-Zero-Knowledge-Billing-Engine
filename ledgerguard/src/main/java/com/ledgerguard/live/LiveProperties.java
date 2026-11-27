package com.ledgerguard.live;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "live")
public class LiveProperties {
    private Duration heartbeat = Duration.ofSeconds(15);
    private Duration maxAge = Duration.ofMinutes(30);
    private int maxStreamsPerUser = 5;
    private int maxStreamsPerTenant = 200;
    private int connectRatePerMinute = 10;
    private int queueSize = 100;
    private int senderThreads = 4;
    private int replaySize = 500;
    private Duration replayWindow = Duration.ofMinutes(10);
    private String redisChannelPrefix = "lg:live:";
    private long goodbyeLeadSeconds = 3;

    public Duration getHeartbeat() { return heartbeat; }
    public void setHeartbeat(Duration heartbeat) { this.heartbeat = heartbeat; }

    public Duration getMaxAge() { return maxAge; }
    public void setMaxAge(Duration maxAge) { this.maxAge = maxAge; }

    public int getMaxStreamsPerUser() { return maxStreamsPerUser; }
    public void setMaxStreamsPerUser(int maxStreamsPerUser) { this.maxStreamsPerUser = maxStreamsPerUser; }

    public int getMaxStreamsPerTenant() { return maxStreamsPerTenant; }
    public void setMaxStreamsPerTenant(int maxStreamsPerTenant) { this.maxStreamsPerTenant = maxStreamsPerTenant; }

    public int getConnectRatePerMinute() { return connectRatePerMinute; }
    public void setConnectRatePerMinute(int connectRatePerMinute) { this.connectRatePerMinute = connectRatePerMinute; }

    public int getQueueSize() { return queueSize; }
    public void setQueueSize(int queueSize) { this.queueSize = queueSize; }

    public int getSenderThreads() { return senderThreads; }
    public void setSenderThreads(int senderThreads) { this.senderThreads = senderThreads; }

    public int getReplaySize() { return replaySize; }
    public void setReplaySize(int replaySize) { this.replaySize = replaySize; }

    public Duration getReplayWindow() { return replayWindow; }
    public void setReplayWindow(Duration replayWindow) { this.replayWindow = replayWindow; }

    public String getRedisChannelPrefix() { return redisChannelPrefix; }
    public void setRedisChannelPrefix(String redisChannelPrefix) { this.redisChannelPrefix = redisChannelPrefix; }

    public long getGoodbyeLeadSeconds() { return goodbyeLeadSeconds; }
    public void setGoodbyeLeadSeconds(long goodbyeLeadSeconds) { this.goodbyeLeadSeconds = goodbyeLeadSeconds; }
}
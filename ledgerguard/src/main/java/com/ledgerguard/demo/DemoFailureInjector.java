package com.ledgerguard.demo;

import com.ledgerguard.ledger.posting.FailureInjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Component
@Profile("demo")
public class DemoFailureInjector implements FailureInjector {

    private static final Logger log = LoggerFactory.getLogger(DemoFailureInjector.class);

    private final Path armFilePath;

    public DemoFailureInjector(@Value("${ledger.demo.failure-file:./.demo/failure}") String filePath) {
        this.armFilePath = Paths.get(filePath);
    }

    @Override
    public void checkpoint(Point point) {
        if (point != null) {
            checkpoint(point.name());
        }
    }

    @Override
    public void checkpoint(String key) {
        if (key == null || !Files.exists(armFilePath)) {
            return;
        }

        Path claimedFile = armFilePath.resolveSibling("failure.claimed." + UUID.randomUUID());
        try {
            Files.move(armFilePath, claimedFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            return;
        }

        String instruction;
        try {
            instruction = Files.readString(claimedFile).trim();
        } catch (IOException e) {
            log.error("Failed to read claimed demo failure file", e);
            return;
        } finally {
            try {
                Files.deleteIfExists(claimedFile);
            } catch (IOException ignored) {}
        }

        if (instruction.isEmpty()) {
            return;
        }

        String[] parts = instruction.split(":");
        if (parts.length < 2) {
            log.warn("Invalid demo failure instruction format: {}", instruction);
            return;
        }

        String targetPoint = parts[0].trim().toUpperCase();
        String action = parts[1].trim().toUpperCase();
        long delayMs = (parts.length >= 3) ? Long.parseLong(parts[2].trim()) : 1000L;

        if (!key.trim().equalsIgnoreCase(targetPoint)) {
            reArm(instruction);
            return;
        }

        log.warn("!!! DEMO FAILURE INJECTOR TRIGGERED: {} -> {} !!!", targetPoint, action);

        switch (action) {
            case "FAIL" -> throw new IllegalStateException("Simulated Demo Failure at " + targetPoint);
            case "CRASH" -> {
                log.error("Simulating hard JVM crash (halt 137) at {}", targetPoint);
                Runtime.getRuntime().halt(137);
            }
            case "DELAY" -> {
                try {
                    log.warn("Sleeping {} ms at {}", delayMs, targetPoint);
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ie);
                }
            }
            default -> log.warn("Unknown demo failure action: {}", action);
        }
    }

    private void reArm(String instruction) {
        try {
            if (armFilePath.getParent() != null) {
                Files.createDirectories(armFilePath.getParent());
            }
            Files.writeString(armFilePath, instruction, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to re-arm failure file for downstream checkpoint", e);
        }
    }
}
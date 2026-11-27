package com.ledgerguard.live;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
@Configuration
@EnableScheduling
@EnableConfigurationProperties(LiveProperties.class)
public class LiveConfig {
}
package com.ledgerlite.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledgerlite.retry")
public record RetryProperties(int maxAttempts, long initialBackoffMs, double multiplier) {
}
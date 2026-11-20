package com.ledgerlite.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String eventType, String payload, Instant createdAt) {
}
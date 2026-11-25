package com.quickbank.common.event;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String idempotencyKey,
        JsonNode payload
) {
    public EventEnvelope {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (eventVersion < 1) {
            throw new IllegalArgumentException("eventVersion starts at 1");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload is required");
        }
    }
}

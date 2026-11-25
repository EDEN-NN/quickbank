package com.quickbank.observability.lab;

import java.time.Instant;
import java.util.Map;

public record CapturedSpan(
        String name,
        long durationMillis,
        String error,
        Map<String, String> attributes,
        Instant recordedAt
) {
}

package com.quickbank.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;

public class EventFactory {

    private final ObjectMapper mapper;
    private final Clock clock;

    public EventFactory(ObjectMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public EventEnvelope create(String eventType, String idempotencyKey, Object payload) {
        return new EventEnvelope(
                UUID.randomUUID(),
                eventType,
                1,
                clock.instant(),
                idempotencyKey,
                mapper.valueToTree(payload));
    }
}

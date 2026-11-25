package com.quickbank.common.event;

import com.quickbank.common.event.payload.DepositPostedPayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventEnvelopeTest {

    @Test
    void roundTripsVersionAndIdempotencyKey() throws Exception {
        var mapper = JsonMappers.events();
        var factory = new EventFactory(mapper, java.time.Clock.fixed(Instant.parse("2026-11-24T18:00:00Z"), java.time.ZoneOffset.UTC));
        var envelope = factory.create(
                EventTypes.DEPOSIT_POSTED,
                "deposit-key-1",
                new DepositPostedPayload("dep-1", "10000001", "25.00", "BRL", "25.00", Instant.parse("2026-11-24T18:00:00Z")));

        String json = mapper.writeValueAsString(envelope);
        assertTrue(json.contains("\"eventVersion\":1"));
        assertTrue(json.contains("\"idempotencyKey\":\"deposit-key-1\""));
        assertTrue(json.contains("\"amount\":\"25.00\""));

        EventEnvelope restored = mapper.readValue(json, EventEnvelope.class);
        DepositPostedPayload payload = mapper.treeToValue(restored.payload(), DepositPostedPayload.class);
        assertEquals(EventTypes.DEPOSIT_POSTED, restored.eventType());
        assertEquals(1, restored.eventVersion());
        assertEquals(new BigDecimal("25.00"), new BigDecimal(payload.amount()));
        assertEquals("10000001", payload.accountNumber());
    }
}

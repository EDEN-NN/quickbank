package com.quickbank.common.outbox;

import com.quickbank.common.event.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
public class OutboxWriter {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public OutboxWriter(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void append(String table, String topic, String messageKey, EventEnvelope event) {
        String allowed = OutboxTables.require(table);
        try {
            jdbc.update("""
                    INSERT INTO %s
                      (event_id, event_type, event_version, topic, message_key, idempotency_key, body, occurred_at)
                    VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?)
                    """.formatted(allowed),
                    event.eventId().toString(),
                    event.eventType(),
                    event.eventVersion(),
                    topic,
                    messageKey,
                    event.idempotencyKey(),
                    mapper.writeValueAsString(event),
                    Timestamp.from(event.occurredAt()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize " + event.eventType(), ex);
        }
    }
}

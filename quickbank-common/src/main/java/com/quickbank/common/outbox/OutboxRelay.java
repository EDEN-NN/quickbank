package com.quickbank.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka) {
        this.jdbc = jdbc;
        this.kafka = kafka;
    }

    @Transactional
    public int flush(String table, int batchSize) {
        String allowed = OutboxTables.require(table);
        List<Row> rows = jdbc.query("""
                SELECT id, topic, message_key, event_type, event_id, idempotency_key, body
                FROM %s
                WHERE published_at IS NULL
                ORDER BY id
                LIMIT ?
                FOR UPDATE SKIP LOCKED
                """.formatted(allowed), (rs, rowNum) -> new Row(
                rs.getLong("id"),
                rs.getString("topic"),
                rs.getString("message_key"),
                rs.getString("event_type"),
                rs.getString("event_id"),
                rs.getString("idempotency_key"),
                body(rs.getObject("body"))), batchSize);
        int published = 0;
        for (Row row : rows) {
            var record = new org.apache.kafka.clients.producer.ProducerRecord<>(row.topic(), row.messageKey(), row.body());
            record.headers().add("event-type", row.eventType().getBytes(StandardCharsets.UTF_8));
            record.headers().add("event-id", row.eventId().getBytes(StandardCharsets.UTF_8));
            record.headers().add("idempotency-key", row.idempotencyKey().getBytes(StandardCharsets.UTF_8));
            try {
                kafka.send(record).get(10, TimeUnit.SECONDS);
            } catch (Exception ex) {
                throw new IllegalStateException("Outbox publish failed for " + row.eventType() + " " + row.eventId(), ex);
            }
            jdbc.update("UPDATE " + allowed + " SET published_at = CURRENT_TIMESTAMP(6) WHERE id = ?", row.id());
            published++;
        }
        if (published > 0) {
            log.debug("Published {} message(s) from {}", published, allowed);
        }
        return published;
    }

    private static String body(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    private record Row(
            long id,
            String topic,
            String messageKey,
            String eventType,
            String eventId,
            String idempotencyKey,
            String body
    ) {
    }
}

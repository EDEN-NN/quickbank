package com.quickbank.payment.app;

import com.quickbank.common.event.EventEnvelope;
import com.quickbank.common.event.EventFactory;
import com.quickbank.common.event.EventTypes;
import com.quickbank.common.event.Topics;
import com.quickbank.common.event.payload.PaymentCompletedPayload;
import com.quickbank.common.event.payload.PaymentRequestedPayload;
import com.quickbank.common.outbox.OutboxTables;
import com.quickbank.common.outbox.OutboxWriter;
import com.quickbank.common.trace.QuickBankSpans;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;

@Service
public class PaymentMockService {

    private static final Logger log = LoggerFactory.getLogger("com.quickbank.mock.payment");

    private final JdbcTemplate jdbc;
    private final OutboxWriter outbox;
    private final EventFactory events;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final QuickBankSpans spans;
    private final TransactionTemplate transaction;
    private final long delayMs;

    public PaymentMockService(
            JdbcTemplate jdbc,
            OutboxWriter outbox,
            EventFactory events,
            ObjectMapper mapper,
            Clock clock,
            QuickBankSpans spans,
            PlatformTransactionManager transactions,
            @Value("${quickbank.payment.mock-delay-ms:750}") long delayMs
    ) {
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
        this.spans = spans;
        this.transaction = new TransactionTemplate(transactions);
        this.delayMs = delayMs;
    }

    public void handle(EventEnvelope event) {
        if (!EventTypes.PAYMENT_REQUESTED.equals(event.eventType())) {
            return;
        }
        PaymentRequestedPayload request = mapper.convertValue(event.payload(), PaymentRequestedPayload.class);
        if (alreadyProcessed(request.paymentId())) {
            log.info("MOCK payment — no PSP. paymentId={} was already approved; skipping a duplicate command.", request.paymentId());
            return;
        }
        spans.run(
                "quickbank.payment.mock",
                Map.of("module", "payment", "mock", "true", "movement.type", "DEPOSIT"),
                Map.of("account.number", request.accountNumber()),
                () -> approve(request));
    }

    private void approve(PaymentRequestedPayload request) {
        log.info(
                "MOCK payment — no PSP was called. Approving paymentId={} account={} amount={} {} after {}ms.",
                request.paymentId(), request.accountNumber(), request.amount(), request.currency(), delayMs);
        try {
            Thread.sleep(Math.max(0, delayMs));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Payment mock interrupted", ex);
        }
        transaction.executeWithoutResult(status -> {
            try {
                jdbc.update(
                        "INSERT INTO pay_inbox (payment_id, processed_at) VALUES (?, ?)",
                        request.paymentId(),
                        Timestamp.from(clock.instant()));
            } catch (DuplicateKeyException ex) {
                log.info("MOCK payment — no PSP. paymentId={} already stored; not publishing a second result.", request.paymentId());
                return;
            }
            outbox.append(
                    OutboxTables.PAYMENT,
                    Topics.PAYMENT_EVENTS,
                    request.accountNumber(),
                    events.create(
                            EventTypes.PAYMENT_COMPLETED,
                            request.idempotencyKey(),
                            new PaymentCompletedPayload(
                                    request.paymentId(),
                                    "SUCCEEDED",
                                    request.accountNumber(),
                                    request.amount(),
                                    request.currency(),
                                    request.idempotencyKey())));
            log.info(
                    "MOCK payment — no PSP. Published SUCCEEDED for paymentId={} account={} amount={} {}.",
                    request.paymentId(), request.accountNumber(), request.amount(), request.currency());
        });
    }

    private boolean alreadyProcessed(String paymentId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pay_inbox WHERE payment_id = ?",
                Integer.class,
                paymentId);
        return count != null && count > 0;
    }
}

package com.quickbank.notification.app;

import com.quickbank.common.event.EventEnvelope;
import com.quickbank.common.event.EventTypes;
import com.quickbank.common.event.payload.SendEmailPayload;
import com.quickbank.common.trace.QuickBankSpans;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;

@Service
public class NotificationMockService {

    private static final Logger log = LoggerFactory.getLogger("com.quickbank.mock.notification");

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final QuickBankSpans spans;
    private final TransactionTemplate transaction;

    public NotificationMockService(
            JdbcTemplate jdbc,
            ObjectMapper mapper,
            Clock clock,
            QuickBankSpans spans,
            PlatformTransactionManager transactions
    ) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.clock = clock;
        this.spans = spans;
        this.transaction = new TransactionTemplate(transactions);
    }

    public void handle(EventEnvelope event) {
        if (!EventTypes.SEND_EMAIL.equals(event.eventType())) {
            return;
        }
        SendEmailPayload payload = mapper.convertValue(event.payload(), SendEmailPayload.class);
        String movement = switch (payload.template() == null ? "" : payload.template()) {
            case "ACCOUNT_OPENED" -> "OPEN";
            case "DEPOSIT_POSTED" -> "DEPOSIT";
            case "TRANSFER_SENT", "TRANSFER_RECEIVED" -> "TRANSFER";
            default -> "EMAIL";
        };
        spans.run(
                "quickbank.notification.mock",
                Map.of("module", "notification", "mock", "true", "movement.type", movement),
                Map.of("account.number", payload.accountNumber() == null ? "unknown" : payload.accountNumber()),
                () -> dispatch(event, payload));
    }

    private void dispatch(EventEnvelope event, SendEmailPayload payload) {
        EmailDraft draft = EmailDraft.from(payload);
        transaction.executeWithoutResult(status -> {
            try {
                jdbc.update(
                        "INSERT INTO ntf_inbox (event_id, template, recipient, processed_at) VALUES (?, ?, ?, ?)",
                        event.eventId().toString(),
                        payload.template(),
                        payload.recipient(),
                        Timestamp.from(clock.instant()));
            } catch (DuplicateKeyException ex) {
                log.info("MOCK email — no SMTP. eventId={} already logged; not sending again.", event.eventId());
                return;
            }
            log.info(
                    "MOCK email — no SMTP delivery. to={} template={} account={} counterparty={} amount={} subject=\"{}\" body=\"{}\"",
                    payload.recipient(),
                    payload.template(),
                    payload.accountNumber(),
                    payload.counterpartyAccount(),
                    payload.amount() == null ? "" : payload.amount() + " " + payload.currency(),
                    draft.subject(),
                    draft.body());
        });
    }
}

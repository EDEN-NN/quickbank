package com.quickbank.account.kafka;

import com.quickbank.account.app.AccountApplicationService;
import com.quickbank.common.event.EventEnvelope;
import com.quickbank.common.event.EventTypes;
import com.quickbank.common.event.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LedgerEventListener {

    private final ObjectMapper mapper;
    private final AccountApplicationService accounts;

    public LedgerEventListener(ObjectMapper mapper, AccountApplicationService accounts) {
        this.mapper = mapper;
        this.accounts = accounts;
    }

    @KafkaListener(topics = Topics.LEDGER_EVENTS, groupId = "quickbank.account.ledger")
    public void onLedgerEvent(String body) throws Exception {
        EventEnvelope event = mapper.readValue(body, EventEnvelope.class);
        if (EventTypes.DEPOSIT_POSTED.equals(event.eventType()) || EventTypes.TRANSFER_POSTED.equals(event.eventType())) {
            accounts.apply(event);
        }
    }
}

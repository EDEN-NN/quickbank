package com.quickbank.ledger.kafka;

import com.quickbank.common.event.EventEnvelope;
import com.quickbank.common.event.EventTypes;
import com.quickbank.common.event.Topics;
import com.quickbank.common.event.payload.AccountOpenedPayload;
import com.quickbank.ledger.app.LedgerApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AccountOpenedListener {

    private static final Logger log = LoggerFactory.getLogger(AccountOpenedListener.class);

    private final ObjectMapper mapper;
    private final LedgerApplicationService ledger;

    public AccountOpenedListener(ObjectMapper mapper, LedgerApplicationService ledger) {
        this.mapper = mapper;
        this.ledger = ledger;
    }

    @KafkaListener(topics = Topics.ACCOUNT_EVENTS, groupId = "quickbank.ledger.accounts")
    public void onAccountEvent(String body) throws Exception {
        EventEnvelope event = mapper.readValue(body, EventEnvelope.class);
        if (!EventTypes.ACCOUNT_OPENED.equals(event.eventType())) {
            return;
        }
        AccountOpenedPayload payload = mapper.treeToValue(event.payload(), AccountOpenedPayload.class);
        ledger.provision(payload);
        log.info("Provisioned ledger account {} for {}", payload.accountNumber(), payload.ownerName());
    }
}

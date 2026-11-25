package com.quickbank.ledger.kafka;

import com.quickbank.common.event.EventEnvelope;
import com.quickbank.common.event.EventTypes;
import com.quickbank.common.event.Topics;
import com.quickbank.common.event.payload.PaymentCompletedPayload;
import com.quickbank.ledger.app.LedgerApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final ObjectMapper mapper;
    private final LedgerApplicationService ledger;

    public PaymentResultListener(ObjectMapper mapper, LedgerApplicationService ledger) {
        this.mapper = mapper;
        this.ledger = ledger;
    }

    @KafkaListener(topics = Topics.PAYMENT_EVENTS, groupId = "quickbank.ledger.payments")
    public void onPaymentEvent(String body) throws Exception {
        EventEnvelope event = mapper.readValue(body, EventEnvelope.class);
        if (!EventTypes.PAYMENT_COMPLETED.equals(event.eventType())) {
            return;
        }
        PaymentCompletedPayload payload = mapper.treeToValue(event.payload(), PaymentCompletedPayload.class);
        ledger.postPayment(payload);
        log.info("Posted deposit from payment {} on account {}", payload.paymentId(), payload.accountNumber());
    }
}

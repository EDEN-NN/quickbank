package com.quickbank.payment.kafka;

import com.quickbank.common.event.EventEnvelope;
import com.quickbank.common.event.Topics;
import com.quickbank.payment.app.PaymentMockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentCommandListener {

    private final ObjectMapper mapper;
    private final PaymentMockService payments;

    public PaymentCommandListener(ObjectMapper mapper, PaymentMockService payments) {
        this.mapper = mapper;
        this.payments = payments;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMMANDS, groupId = "quickbank.payment")
    public void onCommand(String body) throws Exception {
        payments.handle(mapper.readValue(body, EventEnvelope.class));
    }
}

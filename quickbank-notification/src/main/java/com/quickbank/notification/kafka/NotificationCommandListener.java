package com.quickbank.notification.kafka;

import com.quickbank.common.event.EventEnvelope;
import com.quickbank.common.event.Topics;
import com.quickbank.notification.app.NotificationMockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationCommandListener {

    private final ObjectMapper mapper;
    private final NotificationMockService notifications;

    public NotificationCommandListener(ObjectMapper mapper, NotificationMockService notifications) {
        this.mapper = mapper;
        this.notifications = notifications;
    }

    @KafkaListener(topics = Topics.NOTIFICATION_COMMANDS, groupId = "quickbank.notification")
    public void onCommand(String body) throws Exception {
        notifications.handle(mapper.readValue(body, EventEnvelope.class));
    }
}

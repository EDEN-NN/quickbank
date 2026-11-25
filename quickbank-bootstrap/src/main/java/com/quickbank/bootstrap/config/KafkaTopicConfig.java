package com.quickbank.bootstrap.config;

import com.quickbank.common.event.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfig {

    @Bean
    KafkaAdmin.NewTopics quickBankTopics() {
        NewTopic[] topics = Topics.all().stream()
                .map(name -> TopicBuilder.name(name).partitions(1).replicas(1).build())
                .toArray(NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topics);
    }
}

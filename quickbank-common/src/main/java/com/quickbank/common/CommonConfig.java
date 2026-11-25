package com.quickbank.common;

import com.quickbank.common.event.EventFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CommonConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    EventFactory eventFactory(ObjectMapper objectMapper, Clock clock) {
        return new EventFactory(objectMapper, clock);
    }
}

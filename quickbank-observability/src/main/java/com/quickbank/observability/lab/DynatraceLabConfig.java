package com.quickbank.observability.lab;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DynatraceLabConfig {

    @Bean
    ObservationRegistryCustomizer<ObservationRegistry> dynatraceLabHandler(DynatraceLabRecorder recorder) {
        return registry -> registry.observationConfig().observationHandler(recorder);
    }
}

package com.quickbank.observability.otlp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OtlpEnvironmentPostProcessorTest {

    @Test
    void appendsSignalPathsToADynatraceBaseUrl() {
        assertEquals(
                "https://abc.live.dynatrace.com/api/v2/otlp/v1/traces",
                OtlpEnvironmentPostProcessor.tracesUrl("https://abc.live.dynatrace.com/api/v2/otlp"));
        assertEquals(
                "https://abc.live.dynatrace.com/api/v2/otlp/v1/metrics",
                OtlpEnvironmentPostProcessor.metricsUrl("https://abc.live.dynatrace.com/api/v2/otlp/"));
    }
}

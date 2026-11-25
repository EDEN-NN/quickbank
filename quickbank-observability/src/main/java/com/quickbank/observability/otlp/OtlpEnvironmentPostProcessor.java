package com.quickbank.observability.otlp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Points OTLP at Dynatrace when DT_OTLP_ENDPOINT is set, and at the local collector otherwise.
 * The process boots either way. A missing tenant is not an error.
 */
public class OtlpEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dynatraceEndpoint = environment.getProperty("DT_OTLP_ENDPOINT", "");
        String token = environment.getProperty("DT_API_TOKEN", "");
        String tenant = environment.getProperty("DT_TENANT", "");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("quickbank.otlp.tenant", tenant == null ? "" : tenant);
        if (dynatraceEndpoint != null && !dynatraceEndpoint.isBlank()) {
            String traces = tracesUrl(dynatraceEndpoint.trim());
            String metrics = metricsUrl(dynatraceEndpoint.trim());
            properties.put("management.otlp.tracing.endpoint", traces);
            properties.put("management.otlp.metrics.export.url", metrics);
            properties.put("management.otlp.metrics.export.enabled", "true");
            properties.put("quickbank.otlp.target", "dynatrace");
            if (token != null && !token.isBlank()) {
                String header = "Api-Token " + token.trim();
                properties.put("management.otlp.tracing.headers.Authorization", header);
                properties.put("management.otlp.metrics.export.headers.Authorization", header);
            }
        } else {
            properties.put("management.otlp.tracing.endpoint",
                    environment.getProperty("OTLP_TRACES_ENDPOINT", "http://127.0.0.1:4318/v1/traces"));
            properties.put("management.otlp.metrics.export.url",
                    environment.getProperty("OTLP_METRICS_ENDPOINT", "http://127.0.0.1:4318/v1/metrics"));
            properties.put("management.otlp.metrics.export.enabled", "true");
            properties.put("quickbank.otlp.target", "local-collector");
        }
        environment.getPropertySources().addFirst(new MapPropertySource("quickbank-otlp", properties));
    }

    static String tracesUrl(String endpoint) {
        String base = strip(endpoint);
        if (base.endsWith("/v1/traces")) {
            return base;
        }
        if (base.endsWith("/v1/metrics")) {
            return base.substring(0, base.length() - "/v1/metrics".length()) + "/v1/traces";
        }
        return base + "/v1/traces";
    }

    static String metricsUrl(String endpoint) {
        String base = strip(endpoint);
        if (base.endsWith("/v1/metrics")) {
            return base;
        }
        if (base.endsWith("/v1/traces")) {
            return base.substring(0, base.length() - "/v1/traces".length()) + "/v1/metrics";
        }
        return base + "/v1/metrics";
    }

    private static String strip(String endpoint) {
        String value = endpoint;
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}

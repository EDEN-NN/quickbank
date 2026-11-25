package com.quickbank.observability.lab;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DynatraceLab {

    private final DynatraceCatalog catalog;
    private final DynatraceLabRecorder recorder;
    private final String target;
    private final String tenant;

    public DynatraceLab(
            DynatraceCatalog catalog,
            DynatraceLabRecorder recorder,
            @Value("${quickbank.otlp.target:local-collector}") String target,
            @Value("${quickbank.otlp.tenant:}") String tenant
    ) {
        this.catalog = catalog;
        this.recorder = recorder;
        this.target = target;
        this.tenant = tenant;
    }

    public LabView view() {
        boolean dynatrace = "dynatrace".equals(target);
        return new LabView(target, tenant, dynatrace, catalog.spans(), catalog.queries(), recorder.recent());
    }

    public record LabView(
            String exportTarget,
            String tenant,
            boolean dynatraceConfigured,
            List<SpanSpec> spans,
            List<QuerySpec> queries,
            List<CapturedSpan> recent
    ) {
    }
}

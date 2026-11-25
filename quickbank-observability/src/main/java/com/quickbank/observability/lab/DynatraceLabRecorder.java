package com.quickbank.observability.lab;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DynatraceLabRecorder implements ObservationHandler<Observation.Context> {

    private static final int LIMIT = 80;
    private final Deque<CapturedSpan> recent = new ArrayDeque<>();

    @Override
    public void onStart(Observation.Context context) {
        context.put("quickbank.startNanos", System.nanoTime());
    }

    @Override
    public void onStop(Observation.Context context) {
        String name = context.getName();
        if (name == null || !name.startsWith("quickbank.")) {
            return;
        }
        Long start = context.get("quickbank.startNanos");
        long millis = start == null ? 0 : Math.max(0, (System.nanoTime() - start) / 1_000_000);
        Map<String, String> attributes = new LinkedHashMap<>();
        context.getAllKeyValues().forEach(keyValue -> attributes.put(keyValue.getKey(), String.valueOf(keyValue.getValue())));
        String error = context.getError() == null ? null : context.getError().getClass().getSimpleName();
        synchronized (recent) {
            recent.addFirst(new CapturedSpan(name, millis, error, attributes, Instant.now()));
            while (recent.size() > LIMIT) {
                recent.removeLast();
            }
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }

    public List<CapturedSpan> recent() {
        synchronized (recent) {
            return List.copyOf(recent);
        }
    }
}

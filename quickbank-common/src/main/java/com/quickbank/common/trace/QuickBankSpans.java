package com.quickbank.common.trace;

import com.quickbank.common.error.QuickBankException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class QuickBankSpans {

    private final ObservationRegistry registry;

    public QuickBankSpans(ObservationRegistry registry) {
        this.registry = registry;
    }

    public <T> T run(String name, Map<String, String> low, Map<String, String> high, Supplier<T> action) {
        return run(name, low, high, bag -> action.get());
    }

    public <T> T run(String name, Map<String, String> low, Map<String, String> high, Function<SpanBag, T> action) {
        Observation observation = Observation.start(name, registry);
        low.forEach(observation::lowCardinalityKeyValue);
        high.forEach(observation::highCardinalityKeyValue);
        try (Observation.Scope ignored = observation.openScope()) {
            return action.apply(new SpanBag(observation));
        } catch (QuickBankException ex) {
            observation.highCardinalityKeyValue("error.type", ex.typeSlug());
            observation.error(ex);
            throw ex;
        } catch (RuntimeException ex) {
            observation.highCardinalityKeyValue("error.type", ex.getClass().getSimpleName());
            observation.error(ex);
            throw ex;
        } finally {
            observation.stop();
        }
    }

    public void run(String name, Map<String, String> low, Map<String, String> high, Runnable action) {
        run(name, low, high, () -> {
            action.run();
            return null;
        });
    }

    public static final class SpanBag {
        private final Observation observation;

        private SpanBag(Observation observation) {
            this.observation = observation;
        }

        public void attribute(String key, String value) {
            if (value != null) {
                observation.highCardinalityKeyValue(key, value);
            }
        }
    }
}

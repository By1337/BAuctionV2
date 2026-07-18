package dev.by1337.auc.metrics;

import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class Metrics {
    private final Map<String, WindowedMetric> metrics =
            new ConcurrentHashMap<>();
    public Metrics() {
    }

    public WindowedMetric create(
            String name,
            MetricFormatter formatter,
            LongSupplier preTick
    ) {
        WindowedMetric metric = new WindowedMetric(
                formatter,
                preTick
        );

        metrics.put(name, metric);
        return metric;
    }

    public WindowedMetric get(String name) {
        return metrics.get(name);
    }

    public void tick() {
        for (WindowedMetric metric : metrics.values()) {
            metric.tick();
        }
    }

    public void dump(Logger logger) {
        for (Map.Entry<String, WindowedMetric> entry : metrics.entrySet()) {
            logger.info("{}\n{}", entry.getKey(), entry.getValue().snapshot().toString());
        }
    }
}
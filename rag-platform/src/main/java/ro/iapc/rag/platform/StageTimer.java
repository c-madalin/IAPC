package ro.iapc.rag.platform;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import ro.iapc.rag.common.MetricNames;

/**
 * Times a pipeline stage, records it under {@link MetricNames#STAGE_DURATION} and, when given a map, stores the
 * milliseconds under the stage name for the response's {@code timingsMs}.
 */
public class StageTimer {

    private final MeterRegistry meterRegistry;

    public StageTimer(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public <T> T time(String stage, Supplier<T> work) {
        return time(stage, null, work);
    }

    public <T> T time(String stage, Map<String, Long> timingsMs, Supplier<T> work) {
        long start = System.nanoTime();
        try {
            return work.get();
        } finally {
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
            Timer.builder(MetricNames.STAGE_DURATION)
                    .tag(MetricNames.TAG_STAGE, stage)
                    .publishPercentileHistogram()
                    .register(meterRegistry)
                    .record(elapsed);
            if (timingsMs != null) {
                timingsMs.put(stage, elapsed.toMillis());
            }
        }
    }
}

package ro.iapc.rag.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ro.iapc.rag.common.MetricNames;

class StageTimerTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final StageTimer timer = new StageTimer(registry);

    @Test
    void recordsMetricAndTimingOnSuccess() {
        Map<String, Long> timings = new HashMap<>();

        String result = timer.time("rerank", timings, () -> "ok");

        assertThat(result).isEqualTo("ok");
        assertThat(timings).containsKey("rerank");
        assertThat(registry.get(MetricNames.STAGE_DURATION)
                        .tag(MetricNames.TAG_STAGE, "rerank")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void recordsTimingEvenWhenStageThrows() {
        Map<String, Long> timings = new HashMap<>();

        assertThatThrownBy(() -> timer.time("llm", timings, () -> {
                    throw new IllegalStateException("model down");
                }))
                .hasMessage("model down");

        assertThat(timings).containsKey("llm");
    }
}

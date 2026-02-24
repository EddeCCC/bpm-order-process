package demo.camunda.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MeterUtil {

    private final static String WORKER_TIMER_METRIC_NAME = "camunda.worker.duration";

    @Autowired
    private MeterRegistry meterRegistry;

    public Counter createCounter(String name, String... tags) {
        return meterRegistry.counter(name, tags);
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopWorkerTimer(Timer.Sample sample, String type) {
        sample.stop(meterRegistry.timer(WORKER_TIMER_METRIC_NAME, "type", type));
    }
}

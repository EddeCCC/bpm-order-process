package demo.camunda.order.payment;

import demo.camunda.metrics.MeterUtil;
import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final String ERROR_CODE = "PAYMENT_FAILED";

    private final CamundaClient client;

    private final MeterUtil meter;

    private final Counter successCounter;
    private final Counter failCounter;

    @Autowired
    public PaymentService(CamundaClient client, MeterUtil meter) {
        this.client = client;
        this.meter = meter;
        this.successCounter = this.meter.createCounter(
                "camunda.worker.job",
                "type", "handlePayment",
                "status", "success"
        );

        this.failCounter = meter.createCounter(
                "camunda.worker.job",
                "type", "handlePayment",
                "status", "failure"
        );
    }

    @JobWorker(type = "handlePayment")
    public void handleJob(final ActivatedJob job) {
        Timer.Sample sample = this.meter.startTimer();
        Map<String, Object> inputVars = job.getVariablesAsMap();
        log.info("Input variables: {}", inputVars);

        try {
            service();
            successCounter.increment();
            log.info("Payment successful");
        } catch (Exception e) {
            failCounter.increment();
            client.newThrowErrorCommand(job).errorCode(ERROR_CODE).send().join();
        } finally {
            meter.stopWorkerTimer(sample, "handlePayment");
        }
    }

    private void service() {
        boolean error = Math.random() < 0.5;
        if (error) {
            log.error("Payment failed");
            throw new RuntimeException("Service error");
        }
    }
}

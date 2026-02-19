package demo.camunda.order.payment;

import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final String ERROR_CODE = "PAYMENT_FAILED";

    @Autowired
    private CamundaClient client;

    @JobWorker(type = "handlePayment")
    public void handleJob(final ActivatedJob job) {
        Map<String, Object> inputVars = job.getVariablesAsMap();
        log.info("Input variables: {}", inputVars);

        try {
            service();
        } catch (Exception e) {
            client.newThrowErrorCommand(job).errorCode(ERROR_CODE).send().join();
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

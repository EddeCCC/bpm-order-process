package demo.camunda.order.abort;

import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AbortService {
    private static final Logger log = LoggerFactory.getLogger(AbortService.class);

    private static final String ABORT_MESSAGE = "Message_AbortOrder";

    @Autowired
    private CamundaClient client;

    @JobWorker(type = "abortOrder", fetchVariables = {"orderId"})
    public void handleJob(final ActivatedJob job) {
        String orderId = String.valueOf(job.getVariablesAsMap().get("orderId"));

        log.info("Aborting order {} of instance {}", orderId, job.getKey());

        client.newPublishMessageCommand()
                .messageName(ABORT_MESSAGE)
                .correlationKey(orderId)
                .send().join();
    }
}

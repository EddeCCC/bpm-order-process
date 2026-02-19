package demo.camunda.order.distributor;

import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderItemsService {
    private static final Logger log = LoggerFactory.getLogger(OrderItemsService.class);

    private static final String ORDER_MESSAGE = "Message_OrderItems";

    @Autowired
    private CamundaClient client;

    @JobWorker(type = "orderItems", fetchVariables = {"orderId", "items"})
    public void handleJob(final ActivatedJob job) {
        String orderId = String.valueOf(job.getVariablesAsMap().get("orderId"));

        log.info("Ordering required items for {} ", orderId);

        Map<String, Object> outputVars = new HashMap<>(job.getVariablesAsMap());

        client.newPublishMessageCommand()
                .messageName(ORDER_MESSAGE)
                .correlationKey(orderId)
                .variables(outputVars)
                .send().join();
    }
}

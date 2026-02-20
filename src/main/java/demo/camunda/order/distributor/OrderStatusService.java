package demo.camunda.order.distributor;

import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderStatusService {
    private static final Logger log = LoggerFactory.getLogger(OrderStatusService.class);

    private static final String ORDER_MESSAGE = "Message_OrderStatus";

    @Autowired
    private CamundaClient client;

    @JobWorker(type = "sendOrderStatus")
    public void handleJob(final ActivatedJob job) {
        String orderId = String.valueOf(job.getVariable("orderId"));

        Map<String, Object> outputVars = Map.of(
                "orderId", orderId,
                "items", job.getVariable("items"),
                "unavailable", job.getVariable("unavailable")
        );

        log.info("Sending order status for {}", orderId);

        client.newPublishMessageCommand()
                .messageName(ORDER_MESSAGE)
                .correlationKey(orderId)
                .variables(outputVars)
                .send().join();
    }
}

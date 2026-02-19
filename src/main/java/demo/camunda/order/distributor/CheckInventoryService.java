package demo.camunda.order.distributor;

import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CheckInventoryService {
    private static final Logger log = LoggerFactory.getLogger(CheckInventoryService.class);

    @Autowired
    private CamundaClient client;

    @JobWorker(type = "checkInventory")
    public void handleJob(final ActivatedJob job) {
        String orderId = String.valueOf(job.getVariable("orderId"));
        List<Map<String,Object>> items = (List<Map<String, Object>>) job.getVariable("items");

        log.info("Checking inventory for order {}: {}", orderId, items);

        boolean isAvailable = isAvailable();

        client.newCompleteCommand(job)
                .variable("isAvailable", isAvailable)
                .send().join();
    }

    private boolean isAvailable() {
        return Math.random() < 0.5;
    }
}

package demo.camunda.order.distributor;

import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
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

        List<String> unavailableItems = getUnavailableItems(items);
        Map<String, Object> outputVars = Map.of("unavailable", unavailableItems);

        client.newCompleteCommand(job)
                .variables(outputVars)
                .send().join();
    }

    private List<String> getUnavailableItems(List<Map<String,Object>> items) {
        List<String> unavailableItems = new LinkedList<>();
        for (Map<String, Object> item : items) {
            if(isUnavailable()) {
                String name = String.valueOf(item.get("name"));
                unavailableItems.add(name);
            }
        }
        log.info("Unavailable items: {}", unavailableItems);
        return unavailableItems;
    }

    private boolean isUnavailable() {
        return Math.random() < 0.5;
    }
}

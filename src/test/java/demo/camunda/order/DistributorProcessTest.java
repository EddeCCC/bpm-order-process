package demo.camunda.order;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.PublishMessageResponse;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.camunda.process.test.api.assertions.ProcessInstanceSelectors.byProcessId;

@SpringBootTest(properties = {"camunda.client.worker.defaults.enabled=false"})
@CamundaSpringProcessTest
public class DistributorProcessTest {

    @Autowired private CamundaClient client;
    @Autowired private CamundaProcessTestContext processTestContext;

    private final static String PROCESS_ID = "DistributorProcess";

    private final String orderId = "12345";
    private final List<Map<String,Object>> items = List.of(
            Map.of("name", "productA", "unitPrice", "100", "quantity", "2"),
            Map.of("name", "productB", "unitPrice", "200", "quantity", "1")
    );
    private final Map<String, Object> startVars = Map.of("orderId", orderId, "items", items);

    @DisplayName("Ordered items available")
    @Test
    void testHappyPath() {
        startProcess();

        Map<String, Object> variables = Map.of("unavailable", Collections.emptyList());
        processTestContext.mockJobWorker("checkInventory").thenComplete(variables);

        processTestContext.mockJobWorker("sendOrderStatus").thenComplete();

        CamundaAssert.assertThatProcessInstance(byProcessId(PROCESS_ID)).isCompleted();
    }

    @DisplayName("Ordered items not available")
    @Test
    void testItemsNotAvailable() {
        startProcess();

        Map<String, Object> variables = Map.of("unavailable", List.of("product"));
        processTestContext.mockJobWorker("checkInventory").thenComplete(variables);

        CamundaAssert.assertThat(byProcessId(PROCESS_ID)).hasActiveElements("Task_ApproveReorder");
    }

    private PublishMessageResponse startProcess() {
        return client.newPublishMessageCommand()
                .messageName("Message_OrderItems")
                .correlationKey(orderId)
                .variables(startVars)
                .send().join();
    }
}

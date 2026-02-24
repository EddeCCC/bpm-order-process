package demo.camunda.order;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaProcessTestContext;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(properties = {"camunda.client.worker.defaults.enabled=false"})
@CamundaSpringProcessTest
public class OrderProcessTest {

    @Autowired private CamundaClient client;
    @Autowired private CamundaProcessTestContext processTestContext;

    private final static String PROCESS_ID = "OrderProcess";

    private final String orderId = "12345";
    private final Map<String, Object> startVars = Map.of("orderId", orderId);

    @DisplayName("Test deployment of the process")
    @Test
    void testDeployment() {
        final ProcessInstanceEvent processInstance = client.newCreateInstanceCommand()
                .bpmnProcessId(PROCESS_ID)
                .latestVersion()
                .send()
                .join();

        CamundaAssert.assertThat(processInstance).isActive();
    }

    @DisplayName("Order completed successfully")
    @Test
    void testHappyPath() {
        Map<String, Object> input = new HashMap<>(startVars);
        input.put("orderVolume", 50);

        // Start process
        ProcessInstanceEvent processInstance = startInstance(input);

        // Handle payment
        processTestContext.mockJobWorker("handlePayment").thenComplete();

        // Create invoice
        processTestContext.completeUserTask("Task_CreateInvoice");

        // Order items
        processTestContext.mockJobWorker("orderItems").thenComplete();

        // Receive order status
        client.newPublishMessageCommand()
                .messageName("Message_OrderStatus")
                .correlationKey(orderId)
                .send().join();

        // Generate email
        processTestContext.mockJobWorker("io.camunda:http-json:1").thenComplete();

        // Send email
        // jobType extracted from the BPMN file
        processTestContext.mockJobWorker("io.camunda:sendgrid:1").thenComplete();

        CamundaAssert.assertThat(processInstance).hasCompletedElementsInOrder(
                "Task_HandlePayment",
                "Task_CreateInvoice",
                "Task_OrderItems",
                "Task_GenerateEmail",
                "Task_SendEmail"
        );
        CamundaAssert.assertThat(processInstance).isCompleted();
    }

    @DisplayName("Large order volume has to be approved")
    @Test
    void testLargeOrderVolume() {
        Map<String, Object> input = new HashMap<>(startVars);
        input.put("orderVolume", 1000);

        // Start process
        ProcessInstanceEvent processInstance = startInstance(input);

        CamundaAssert.assertThat(processInstance).hasActiveElements("Task_ApproveOrderAI");
    }

    @DisplayName("Order not approved")
    @Test
    void testOrderNotApproved() {
        ProcessInstanceEvent processInstance = startInstanceBefore(startVars, "Task_ApproveOrderAI");

        Map<String, Object> variables = Map.of("approval", Map.of("isApproved", false));
        processTestContext.mockJobWorker("io.camunda.agenticai:aiagent:1").thenComplete(variables);

        CamundaAssert.assertThat(processInstance)
                .hasCompletedElement("EndEvent_OrderCancelled", 1);
        CamundaAssert.assertThat(processInstance).isCompleted();
    }

    @DisplayName("Order manually approved")
    @Test
    void testOrderApproved() {
        ProcessInstanceEvent processInstance = startInstanceBefore(startVars, "Task_ApproveOrderAI");

        Map<String, Object> variables = Map.of("approval", Map.of("isApproved", true));
        processTestContext.mockJobWorker("io.camunda.agenticai:aiagent:1").thenComplete(variables);

        CamundaAssert.assertThat(processInstance).hasActiveElements("Task_HandlePayment");
    }

    @DisplayName("Payment handled manually")
    @Test
    void testPaymentManually() {
        ProcessInstanceEvent processInstance = startInstanceBefore(startVars, "Task_HandlePayment");

        processTestContext.mockJobWorker("handlePayment").thenThrowBpmnError("PAYMENT_FAILED");
        CamundaAssert.assertThat(processInstance).hasActiveElements("Task_ManuallyHandlePayment");

        processTestContext.completeUserTask("Task_ManuallyHandlePayment");
        CamundaAssert.assertThat(processInstance).hasActiveElements("Task_CreateInvoice");
    }

    @DisplayName("Process completed after aborted order")
    @Test
    void testAbortOrder() {
        ProcessInstanceEvent processInstance = startInstance(startVars);

        client.newPublishMessageCommand()
                .messageName("Message_AbortOrder")
                .correlationKey(orderId)
                .send().join();

        CamundaAssert.assertThat(processInstance)
                .hasCompletedElement("EndEvent_OrderAborted_OrderCancelled", 1);
        CamundaAssert.assertThat(processInstance).isCompleted();
    }

    @DisplayName("Abort manually-handle-payment after timeout")
    @Test
    void testAbortManuallyHandlePayment() {
        ProcessInstanceEvent processInstance = startInstanceBefore(startVars,
                "Task_ManuallyHandlePayment");

        processTestContext.increaseTime(Duration.ofMinutes(2));

        CamundaAssert.assertThat(processInstance)
                .hasActiveElements("Event_ManuallyHandlePayment_AbortOrder");
    }

    /**
     * @return the process instance started at the beginning of the process
     */
    private ProcessInstanceEvent startInstance(Map<String, Object> variables) {
        ProcessInstanceEvent processInstance = client.newCreateInstanceCommand()
                .bpmnProcessId(PROCESS_ID)
                .latestVersion()
                .variables(variables)
                .send().join();

        CamundaAssert.assertThat(processInstance).isCreated();

        return processInstance;
    }

    /**
     * @return the process instance started before the provided starting point
     */
    private ProcessInstanceEvent startInstanceBefore(Map<String, Object> variables, String startingPoint) {
        ProcessInstanceEvent processInstance = client
                .newCreateInstanceCommand()
                .bpmnProcessId("OrderProcess")
                .latestVersion()
                .variables(variables)
                .startBeforeElement(startingPoint)
                .send().join();

        CamundaAssert.assertThat(processInstance).isCreated();

        return processInstance;
    }
}

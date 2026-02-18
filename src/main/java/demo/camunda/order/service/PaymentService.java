package demo.camunda.order.service;

import io.camunda.client.CamundaClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private CamundaClient client;

    @PostConstruct
    void test() {
        log.info("Test: " + client.newTopologyRequest().send().join());
    }
}

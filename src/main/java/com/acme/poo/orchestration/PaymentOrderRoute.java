package com.acme.poo.orchestration;

import com.acme.poo.domain.PaymentOrder;
import com.acme.poo.domain.PaymentOrderRepository;
import com.acme.poo.domain.PaymentOrderStatus;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class PaymentOrderRoute extends RouteBuilder {

    private static final String EXECUTE_TOPIC = "payment.execute";
    private static final String DLT_TOPIC = "payment.execute.dlt";

    private final PaymentOrderRepository repository;

    public PaymentOrderRoute(PaymentOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void configure() {

        /* =========================================================
           GLOBAL ERROR HANDLING (Retries + DLT + FAILED state)
           ========================================================= */

        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(5)
                .redeliveryDelay(1000)
                .backOffMultiplier(2.0)
                .useExponentialBackOff()
                .log("Execution failed for ${body}. Marking FAILED and sending to DLT. Error: ${exception.message}")

                // Mark order as FAILED
                .process(exchange -> {
                    String id = exchange.getMessage().getBody(String.class);
                    repository.findById(id).ifPresent(order -> {
                        PaymentOrder failed = new PaymentOrder(
                                order.getId(),
                                order.getDebtorAccount(),
                                order.getCreditorAccount(),
                                order.getAmount(),
                                order.getCurrency(),
                                PaymentOrderStatus.FAILED
                        );
                        repository.save(failed);
                    });
                })

                // Send to Dead Letter Topic
                .to("kafka:" + DLT_TOPIC
                        + "?brokers=localhost:9092"
                        + "&valueSerializer=org.apache.kafka.common.serialization.StringSerializer");

        /* =========================================================
           PAYMENT ORCHESTRATION (HTTP → Camel → Mongo → Kafka)
           ========================================================= */

        from("direct:processPaymentOrder")
                .routeId("payment-order-processing")
                .log("Processing payment order ${body.id}")

                // VALIDATED
                .process(e -> {
                    PaymentOrder o = e.getMessage().getBody(PaymentOrder.class);
                    PaymentOrder updated = new PaymentOrder(
                            o.getId(), o.getDebtorAccount(), o.getCreditorAccount(),
                            o.getAmount(), o.getCurrency(), PaymentOrderStatus.VALIDATED
                    );
                    repository.save(updated);
                    e.getMessage().setBody(updated);
                })
                .log("Payment order ${body.id} validated")

                // ACCEPTED
                .process(e -> {
                    PaymentOrder o = e.getMessage().getBody(PaymentOrder.class);
                    PaymentOrder updated = new PaymentOrder(
                            o.getId(), o.getDebtorAccount(), o.getCreditorAccount(),
                            o.getAmount(), o.getCurrency(), PaymentOrderStatus.ACCEPTED
                    );
                    repository.save(updated);
                    e.getMessage().setBody(updated);
                })
                .log("Payment order ${body.id} accepted")

                // EXECUTING + SEND TO KAFKA
                .process(e -> {
                    PaymentOrder o = e.getMessage().getBody(PaymentOrder.class);
                    PaymentOrder updated = new PaymentOrder(
                            o.getId(), o.getDebtorAccount(), o.getCreditorAccount(),
                            o.getAmount(), o.getCurrency(), PaymentOrderStatus.EXECUTING
                    );
                    repository.save(updated);
                    e.getMessage().setBody(updated.getId()); // Kafka payload = orderId
                })
                .to("kafka:" + EXECUTE_TOPIC
                        + "?brokers=localhost:9092"
                        + "&valueSerializer=org.apache.kafka.common.serialization.StringSerializer")
                .log("Payment order ${body} sent for execution");

        /* =========================================================
           PAYMENT EXECUTION (Kafka → Camel → Mongo)
           ========================================================= */

        from("kafka:" + EXECUTE_TOPIC
                + "?brokers=localhost:9092"
                + "&groupId=payment-executor"
                + "&autoOffsetReset=earliest"
                + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer")
                .routeId("payment-order-execution")
                .log("Executor received payment order ${body}")

                // Optional delay for demo/testing
                // .delay(30000)
                // Simulating a failure
                // .throwException(new RuntimeException("Simulated execution failure"))

                .process(e -> {
                    String id = e.getMessage().getBody(String.class);
                    PaymentOrder order = repository.findById(id)
                            .orElseThrow(() -> new IllegalStateException("Order not found: " + id));

                    PaymentOrder executed = new PaymentOrder(
                            order.getId(),
                            order.getDebtorAccount(),
                            order.getCreditorAccount(),
                            order.getAmount(),
                            order.getCurrency(),
                            PaymentOrderStatus.EXECUTED
                    );

                    repository.save(executed);
                })
                .log("Payment order ${body} marked EXECUTED");

        /* =========================================================
           DEAD LETTER TOPIC CONSUMER (Visibility only)
           ========================================================= */

        from("kafka:" + DLT_TOPIC
                + "?brokers=localhost:9092"
                + "&groupId=payment-executor-dlt"
                + "&autoOffsetReset=earliest"
                + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer")
                .routeId("payment-order-dlt")
                .log("DLT received payment order ${body} (manual intervention required)");
    }
}
package com.acme.poo.orchestration;

import com.acme.poo.domain.PaymentOrder;
import com.acme.poo.domain.PaymentOrderRepository;
import com.acme.poo.domain.PaymentOrderStatus;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class PaymentOrderRoute extends RouteBuilder {

    private final PaymentOrderRepository repository;

    public PaymentOrderRoute(PaymentOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void configure() {

        /* =========================================================
           PAYMENT ORCHESTRATION (HTTP → Camel → Mongo → Kafka)
           ========================================================= */

        from("direct:processPaymentOrder")
                .routeId("payment-order-processing")
                .log("Processing payment order ${body.id}")

                // -------- VALIDATED --------
                .process(exchange -> {
                    PaymentOrder o = exchange.getMessage().getBody(PaymentOrder.class);
                    PaymentOrder updated = new PaymentOrder(
                            o.getId(),
                            o.getDebtorAccount(),
                            o.getCreditorAccount(),
                            o.getAmount(),
                            o.getCurrency(),
                            PaymentOrderStatus.VALIDATED
                    );
                    repository.save(updated);
                    exchange.getMessage().setBody(updated);
                })
                .log("Payment order ${body.id} validated")

                // -------- ACCEPTED --------
                .process(exchange -> {
                    PaymentOrder o = exchange.getMessage().getBody(PaymentOrder.class);
                    PaymentOrder updated = new PaymentOrder(
                            o.getId(),
                            o.getDebtorAccount(),
                            o.getCreditorAccount(),
                            o.getAmount(),
                            o.getCurrency(),
                            PaymentOrderStatus.ACCEPTED
                    );
                    repository.save(updated);
                    exchange.getMessage().setBody(updated);
                })
                .log("Payment order ${body.id} accepted")

                // -------- EXECUTING + SEND TO KAFKA --------
                .process(exchange -> {
                    PaymentOrder o = exchange.getMessage().getBody(PaymentOrder.class);
                    PaymentOrder updated = new PaymentOrder(
                            o.getId(),
                            o.getDebtorAccount(),
                            o.getCreditorAccount(),
                            o.getAmount(),
                            o.getCurrency(),
                            PaymentOrderStatus.EXECUTING
                    );
                    repository.save(updated);

                    // Send only the orderId to Kafka (String)
                    exchange.getMessage().setBody(updated.getId());
                })
                .to("kafka:payment.execute"
                        + "?brokers=localhost:9092"
                        + "&valueSerializer=org.apache.kafka.common.serialization.StringSerializer")
                .log("Payment order ${body} sent for execution");


        /* =========================================================
           PAYMENT EXECUTION (Kafka → Camel → Mongo)
           ========================================================= */

        from("kafka:payment.execute"
                + "?brokers=localhost:9092"
                + "&groupId=payment-executor"
                + "&autoOffsetReset=earliest"
                + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer")
                .routeId("payment-order-execution")
                .log("Executor received payment order ${body}")
                // Artificial delay to observe async behavior (e.g., 30 seconds)
                .delay(30000)
                .process(exchange -> {
                    String paymentOrderId = exchange.getMessage().getBody(String.class);

                    PaymentOrder order = repository.findById(paymentOrderId).orElse(null);
                    if (order == null) {
                        return;
                    }

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
    }
}
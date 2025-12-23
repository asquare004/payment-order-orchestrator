package com.acme.poo.processor.orchestration;

import com.acme.poo.processor.domain.PaymentOrder;
import com.acme.poo.processor.domain.PaymentOrderRepository;
import com.acme.poo.processor.domain.PaymentOrderStatus;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class PaymentExecutionRoute extends RouteBuilder {

    private static final String EXECUTE_TOPIC = "payment.execute";
    private static final String DLT_TOPIC = "payment.execute.dlt";

    private final PaymentOrderRepository repository;

    public PaymentExecutionRoute(PaymentOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void configure() {

        onException(Exception.class)
                .handled(true)
                .maximumRedeliveries(5)
                .redeliveryDelay(1000)
                .useExponentialBackOff()
                .log("Execution failed for ${body}. Marking FAILED.")

                .process(e -> {
                    String id = e.getMessage().getBody(String.class);
                    repository.findById(id).ifPresent(order -> {
                        repository.save(new PaymentOrder(
                                order.getId(),
                                order.getDebtorAccount(),
                                order.getCreditorAccount(),
                                order.getAmount(),
                                order.getCurrency(),
                                PaymentOrderStatus.FAILED
                        ));
                    });
                })
                .to("kafka:" + DLT_TOPIC
                        + "?brokers={{kafka.brokers}}"
                        + "&valueSerializer=org.apache.kafka.common.serialization.StringSerializer");

        from("kafka:" + EXECUTE_TOPIC
                + "?brokers={{kafka.brokers}}"
                + "&groupId=payment-executor"
                + "&autoOffsetReset=earliest"
                + "&consumersCount=3"
                + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer")
                .routeId("payment-o+ \"&consumersCount=3\"rder-execution")
                .log("Executor thread=${threadName} received payment order ${body}")

                // Delay to test concurrency
                .delay(15000)
                .log("Executor thread=${threadName} finished delay for ${body}")

                .process(e -> {
                    String id = e.getMessage().getBody(String.class);
                    PaymentOrder order = repository.findById(id)
                            .orElseThrow(() -> new IllegalStateException("Order not found: " + id));

                    repository.save(new PaymentOrder(
                            order.getId(),
                            order.getDebtorAccount(),
                            order.getCreditorAccount(),
                            order.getAmount(),
                            order.getCurrency(),
                            PaymentOrderStatus.EXECUTED
                    ));
                })
                .log("Payment order ${body} marked EXECUTED");

        from("kafka:" + DLT_TOPIC
                + "?brokers={{kafka.brokers}}"
                + "&groupId=payment-executor-dlt"
                + "&autoOffsetReset=earliest"
                + "&consumersCount=2"
                + "&valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer")
                .routeId("payment-order-dlt")
                .log("DLT received payment order ${body}");
    }
}
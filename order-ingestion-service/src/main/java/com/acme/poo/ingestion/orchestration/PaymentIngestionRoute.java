package com.acme.poo.ingestion.orchestration;

import com.acme.poo.ingestion.domain.PaymentOrder;
import com.acme.poo.ingestion.domain.PaymentOrderRepository;
import com.acme.poo.ingestion.domain.PaymentOrderStatus;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class PaymentIngestionRoute extends RouteBuilder {

    private static final String EXECUTE_TOPIC = "payment.execute";
    private final PaymentOrderRepository repository;

    public PaymentIngestionRoute(PaymentOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void configure() {

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
                    e.getMessage().setBody(updated.getId());
                })
                .to("kafka:" + EXECUTE_TOPIC
                        + "?brokers={{kafka.brokers}}"
                        + "&valueSerializer=org.apache.kafka.common.serialization.StringSerializer")
                .log("Payment order ${body} sent for execution");
    }
}
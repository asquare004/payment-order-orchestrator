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

        from("direct:processPaymentOrder")
                .routeId("payment-order-processing")
                .log("Processing payment order ${body.id}")

                // Step 1: VALIDATE
                .process(exchange -> {
                    PaymentOrder order = exchange.getMessage().getBody(PaymentOrder.class);
                    order = new PaymentOrder(
                            order.getId(),
                            order.getDebtorAccount(),
                            order.getCreditorAccount(),
                            order.getAmount(),
                            order.getCurrency(),
                            PaymentOrderStatus.VALIDATED
                    );
                    repository.save(order);
                    exchange.getMessage().setBody(order);
                })
                .log("Payment order ${body.id} validated")

                // Step 2: ACCEPT
                .process(exchange -> {
                    PaymentOrder order = exchange.getMessage().getBody(PaymentOrder.class);
                    order = new PaymentOrder(
                            order.getId(),
                            order.getDebtorAccount(),
                            order.getCreditorAccount(),
                            order.getAmount(),
                            order.getCurrency(),
                            PaymentOrderStatus.ACCEPTED
                    );
                    repository.save(order);
                    exchange.getMessage().setBody(order);
                })
                .log("Payment order ${body.id} accepted");
    }
}
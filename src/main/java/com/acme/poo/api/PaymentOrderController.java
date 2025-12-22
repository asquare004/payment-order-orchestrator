package com.acme.poo.api;

import com.acme.poo.domain.PaymentOrderStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment-orders")
public class PaymentOrderController {

    @PostMapping
    public CreatePaymentOrderResponse create(@Valid @RequestBody CreatePaymentOrderRequest req) {
        // For now: generate an ID and mark as RECEIVED.
        // Next step: persist to Mongo + orchestrate with Camel/Kafka.
        String id = UUID.randomUUID().toString();
        return new CreatePaymentOrderResponse(id, PaymentOrderStatus.RECEIVED);
    }
}
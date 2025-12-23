package com.acme.poo.api;

import com.acme.poo.domain.*;

import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/payment-orders")
public class PaymentOrderController {

    private final PaymentOrderRepository orderRepo;
    private final IdempotencyRecordRepository idemRepo;

    public PaymentOrderController(PaymentOrderRepository orderRepo, IdempotencyRecordRepository idemRepo) {
        this.orderRepo = orderRepo;
        this.idemRepo = idemRepo;
    }

    @PostMapping
    public CreatePaymentOrderResponse create(
            @RequestHeader(name = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody CreatePaymentOrderRequest req
    ) {
        Optional<IdempotencyRecord> existing = idemRepo.findByKey(idempotencyKey);
        if (existing.isPresent()) {
            String existingOrderId = existing.get().getPaymentOrderId();
            PaymentOrder existingOrder = orderRepo.findById(existingOrderId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Idempotency key exists but referenced order is missing"
                    ));
            return new CreatePaymentOrderResponse(existingOrder.getId(), existingOrder.getStatus());
        }

        String id = UUID.randomUUID().toString();

        PaymentOrder order = new PaymentOrder(
                id,
                req.debtorAccount,
                req.creditorAccount,
                req.amount,
                req.currency,
                PaymentOrderStatus.RECEIVED
        );

        orderRepo.save(order);
        idemRepo.save(new IdempotencyRecord(idempotencyKey, id));

        return new CreatePaymentOrderResponse(id, order.getStatus());
    }

    @GetMapping("/{id}")
    public GetPaymentOrderResponse get(@PathVariable String id) {
        PaymentOrder order = orderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment order not found"));

        return new GetPaymentOrderResponse(
                order.getId(),
                order.getDebtorAccount(),
                order.getCreditorAccount(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus()
        );
    }
}
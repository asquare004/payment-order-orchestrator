package com.acme.poo.ingestion.controller;

import com.acme.poo.ingestion.domain.IdempotencyRecord;
import com.acme.poo.ingestion.domain.IdempotencyRepository;
import com.acme.poo.ingestion.domain.PaymentOrder;
import com.acme.poo.ingestion.domain.PaymentOrderRepository;
import com.acme.poo.ingestion.domain.PaymentOrderStatus;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/payment-orders")
public class PaymentOrderController {

    private final ProducerTemplate producerTemplate;
    private final PaymentOrderRepository repository;
    private final IdempotencyRepository idempotencyRepository;

    public PaymentOrderController(
            ProducerTemplate producerTemplate,
            PaymentOrderRepository repository,
            IdempotencyRepository idempotencyRepository
    ) {
        this.producerTemplate = producerTemplate;
        this.repository = repository;
        this.idempotencyRepository = idempotencyRepository;
    }

    @PostMapping
    public ResponseEntity<PaymentOrder> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreatePaymentOrderRequest req
    ) {
        // Normalize header
        String key = (idempotencyKey == null) ? null : idempotencyKey.trim();
        if (key != null && key.isEmpty()) key = null;

        // If key exists and already seen, return the existing order (idempotent behavior)
        if (key != null) {
            Optional<IdempotencyRecord> existing = idempotencyRepository.findById(key);
            if (existing.isPresent()) {
                String existingOrderId = existing.get().getOrderId();

                // If the order exists, return it (200 OK)
                Optional<PaymentOrder> existingOrder = repository.findById(existingOrderId);
                if (existingOrder.isPresent()) {
                    return ResponseEntity.ok(existingOrder.get());
                }
            }
        }

        // Create a new order
        String id = UUID.randomUUID().toString();

        PaymentOrder order = new PaymentOrder(
                id,
                req.debtorAccount(),
                req.creditorAccount(),
                req.amount(),
                req.currency(),
                PaymentOrderStatus.PENDING
        );

        // Persist initial state
        repository.save(order);

        // Persist idempotency mapping (only if header was provided)
        if (key != null) {
            idempotencyRepository.save(new IdempotencyRecord(key, id, Instant.now()));
        }

        // Start orchestration via Camel route (your existing approach)
        PaymentOrder updated = producerTemplate.requestBody(
                "direct:processPaymentOrder",
                order,
                PaymentOrder.class
        );

        return ResponseEntity
                .created(URI.create("/payment-orders/" + id))
                .body(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return repository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("message", "Payment order not found", "id", id)));
    }

    // Simple DTO (beginner-friendly). We'll upgrade to validation + records later in Phase B.
    public record CreatePaymentOrderRequest(
            String debtorAccount,
            String creditorAccount,
            BigDecimal amount,
            String currency
    ) {}
}

package com.acme.poo.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private String id;

    @Indexed(unique = true)
    private String key;

    private String paymentOrderId;

    private Instant createdAt;

    protected IdempotencyRecord() {
        // for Mongo
    }

    public IdempotencyRecord(String key, String paymentOrderId) {
        this.key = key;
        this.paymentOrderId = paymentOrderId;
        this.createdAt = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public String getPaymentOrderId() {
        return paymentOrderId;
    }
}
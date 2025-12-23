package com.acme.poo.ingestion.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "idempotency_keys")
public class IdempotencyRecord {

    @Id
    private String key;

    @Indexed
    private String orderId;

    private Instant createdAt;

    protected IdempotencyRecord() {}

    public IdempotencyRecord(String key, String orderId, Instant createdAt) {
        this.key = key;
        this.orderId = orderId;
        this.createdAt = createdAt;
    }

    public String getKey() { return key; }
    public String getOrderId() { return orderId; }
    public Instant getCreatedAt() { return createdAt; }
}
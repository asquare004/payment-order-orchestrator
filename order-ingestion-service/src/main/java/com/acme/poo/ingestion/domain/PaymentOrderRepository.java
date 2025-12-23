package com.acme.poo.ingestion.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentOrderRepository
        extends MongoRepository<PaymentOrder, String> {
}
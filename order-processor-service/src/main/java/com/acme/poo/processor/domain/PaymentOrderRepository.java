package com.acme.poo.processor.domain;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentOrderRepository
        extends MongoRepository<PaymentOrder, String> {
}
package com.acme.poo.api;

import com.acme.poo.domain.PaymentOrderStatus;

public class CreatePaymentOrderResponse {
    public String id;
    public PaymentOrderStatus status;

    public CreatePaymentOrderResponse(String id, PaymentOrderStatus status) {
        this.id = id;
        this.status = status;
    }
}
package com.acme.poo.messaging;

public class PaymentExecuteCommand {
    public String paymentOrderId;

    public PaymentExecuteCommand() {}

    public PaymentExecuteCommand(String paymentOrderId) {
        this.paymentOrderId = paymentOrderId;
    }
}
package com.acme.poo.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document(collection = "payment_orders")
public class PaymentOrder {

    @Id
    private String id;

    private String debtorAccount;
    private String creditorAccount;
    private BigDecimal amount;
    private String currency;

    private PaymentOrderStatus status;

    protected PaymentOrder() {
        // for Mongo
    }

    public PaymentOrder(
            String id,
            String debtorAccount,
            String creditorAccount,
            BigDecimal amount,
            String currency,
            PaymentOrderStatus status
    ) {
        this.id = id;
        this.debtorAccount = debtorAccount;
        this.creditorAccount = creditorAccount;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public String getId() { return id; }

    public PaymentOrderStatus getStatus() { return status; }
    public String getDebtorAccount() { return debtorAccount; }
    public String getCreditorAccount() { return creditorAccount; }
    public java.math.BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
}
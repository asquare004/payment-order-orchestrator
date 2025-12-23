package com.acme.poo.api;

import com.acme.poo.domain.PaymentOrderStatus;
import java.math.BigDecimal;

public class GetPaymentOrderResponse {
    public String id;
    public String debtorAccount;
    public String creditorAccount;
    public BigDecimal amount;
    public String currency;
    public PaymentOrderStatus status;

    public GetPaymentOrderResponse(
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
}

package com.acme.poo.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreatePaymentOrderRequest {

    @NotBlank
    public String debtorAccount;

    @NotBlank
    public String creditorAccount;

    @NotNull
    public BigDecimal amount;

    @NotBlank
    public String currency;
}
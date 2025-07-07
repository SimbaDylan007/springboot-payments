package com.yourcompany.payments.dto.smilepay;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

// DTO for the request from your frontend to your backend to start a SmilePay payment
public record PaymentInitiateRequest(
        @NotBlank String billerId,
        @NotBlank String customerAccount,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String paymentMethod,
        // Optional fields for user details
        String firstName,
        String lastName,
        String email,
        String mobilePhoneNumber
) {}
package com.yourcompany.payments.dto.smilepay;

// DTO for the response from your backend to your frontend
public record PaymentInitiateResponse(
        String paymentUrl,
        String orderReference
) {}
package com.yourcompany.payments.dto.smilepay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

// DTO for the incoming webhook from SmilePay
@JsonIgnoreProperties(ignoreUnknown = true) // Safely ignore fields we don't need
public record SmilePayWebhookPayload(
        String orderReference,
        String transactionReference,
        String status,
        String billerId, // Assuming SmilePay can send this back
        String customerAccount, // Assuming SmilePay can send this back
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String userId, // Assuming SmilePay can send this back
        String token
) {}
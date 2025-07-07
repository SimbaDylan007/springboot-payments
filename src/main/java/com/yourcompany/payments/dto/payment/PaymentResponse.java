package com.yourcompany.payments.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Defines the data structure RETURNED BY THE SERVER after processing a payment request.
 * This DTO aggregates all potential fields from a payment operation outcome.
 *
 * @JsonInclude(JsonInclude.Include.NON_NULL) ensures that any fields with null values
 * are not included in the final JSON response, keeping it clean.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        // --- Core Status and Identifiers ---
        boolean successful,
        String status,
        String message,
        String gatewayReference,
        String paymentReference,
        String receiptNumber,

        // --- Biller & Customer Information ---
        String billerId,
        String customerAccount,
        String customerName,

        // --- Financial Details ---
        BigDecimal amount,
        String currency,

        // --- Context & Timestamps ---
        String source,
        String paymentMethod,
        String paymentType,
        LocalDateTime paymentDate,

        // --- Additional Gateway/Biller Details ---
        String receiptDetails,
        String narrative,
        String tokenDetails, // Specifically for billers like ZETDC

        // --- User Identifier ---
        String userId, // The User's public UUID as a string

        // --- Optional Details Echoed Back or Added ---
        String customerPaymentDetails1,
        String customerPaymentDetails2,
        String customerPaymentDetails3,
        String customerPaymentDetails4
) {}
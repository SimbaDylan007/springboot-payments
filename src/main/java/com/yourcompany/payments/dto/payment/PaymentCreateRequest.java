package com.yourcompany.payments.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Defines the data structure REQUIRED FROM THE CLIENT to initiate a payment request.
 * This DTO is used as the @RequestBody for payment endpoints.
 * It includes validation rules that are automatically checked by Spring.
 */
public record PaymentCreateRequest(

        // --- Core Fields Required for Most Payments ---

        @NotBlank(message = "Customer account number is required")
        @Size(min = 3, max = 50, message = "Account number must be between 3 and 50 characters")
        @JsonProperty("customer_account_number") // Allows frontend to send "customer_account_number" or "customerAccountNumber"
        String customerAccountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be a positive value")
        BigDecimal amount,

        @NotBlank(message = "Payment method is required")
        String paymentMethod,

        // --- Optional Core Fields ---

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency, // Not marked @NotBlank, so it's optional. Default can be handled in service.

        String billerId, // Optional, as it can be inferred from the controller path (e.g., /api/v1/cimas)

        // --- Optional Biller-Specific Fields ---

        @JsonProperty("months") // For billers like Nyaradzo
        @Min(value = 1, message = "Months must be at least 1")
        @Max(value = 12, message = "Months cannot be more than 12")
        Integer months,

        @JsonProperty("customer_payment_details1")
        String customerPaymentDetails1,

        @JsonProperty("customer_payment_details2")
        String customerPaymentDetails2,

        @JsonProperty("customer_payment_details3")
        String customerPaymentDetails3,

        @JsonProperty("customer_payment_details4")
        String customerPaymentDetails4,

        @JsonProperty("customer_payment_details5")
        String customerPaymentDetails5,

        @JsonProperty("payment_reference")
        String paymentReference // For post-payment processing scenarios (e.g., ZETDC)
) {}
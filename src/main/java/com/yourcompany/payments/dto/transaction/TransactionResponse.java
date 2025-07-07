package com.yourcompany.payments.dto.transaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for representing a single transaction in a user's history.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(
        Integer id,
        LocalDateTime createdAt,
        String billerId,
        BigDecimal amount,
        String customerAccount,
        String status,
        String paymentReference,
        String paymentMethod,
        String currency,
        String token,
        String receiptNumber
) {}
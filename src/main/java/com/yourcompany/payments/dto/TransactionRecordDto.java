package com.yourcompany.payments.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionRecordDto(
        UUID id,
        UUID userId,
        String operationType,
        LocalDateTime timestamp,
        String clientReference,
        String billerId,
        String customerAccountNumber,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String requestDetails1,
        String requestDetails2,
        String requestDetails3,
        String status,
        boolean successfulFlag,
        String gatewayTransactionId,
        String gatewayMessage,
        String tokenDetails,
        String errorDetails
) {}
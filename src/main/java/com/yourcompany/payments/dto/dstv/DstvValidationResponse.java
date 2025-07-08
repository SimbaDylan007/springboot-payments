package com.yourcompany.payments.dto.dstv;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record DstvValidationResponse(
        @JsonProperty("customerAccount") String accountNumber,
        String customerName,
        @JsonProperty("dueAmount") String dueAmount,
        String currency,
        @JsonProperty("dueDate") String dueDate,
        boolean successful,
        String error
) {}
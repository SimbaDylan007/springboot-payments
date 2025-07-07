package com.yourcompany.payments.dto.dstv;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record DstvValidationResponse(
        @JsonProperty("customerAccount") String accountNumber,
        String customerName,
        @JsonProperty("dueAmount") BigDecimal dueAmount,
        String currency,
        @JsonProperty("dueDate") String dueDate,
        boolean successful,
        String error
) {}
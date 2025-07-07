package com.yourcompany.payments.dto.doves;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record DovesValidationResponse(
        @JsonProperty("customerAccount") String customerAccount,
        @JsonProperty("customerName") String customerName,
        @JsonProperty("premiumAmount") BigDecimal premiumAmount,
        @JsonProperty("isActive") boolean isActive,
        String status,
        boolean successful,
        String error
) {}
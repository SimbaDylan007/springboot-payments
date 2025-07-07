package com.yourcompany.payments.dto.cimas;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CimasValidationResponse(
        @JsonProperty("reference_number") String referenceNumber,
        @JsonProperty("account_type") String accountType,
        @JsonProperty("customer_name") String customerName,
        @JsonProperty("current_balance") BigDecimal currentBalance,
        String currency,
        String status
) {}
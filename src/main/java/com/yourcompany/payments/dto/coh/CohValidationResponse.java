package com.yourcompany.payments.dto.coh;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CohValidationResponse(
        @JsonProperty("account_number") String accountNumber,
        @JsonProperty("customer_name") String customerName,
        @JsonProperty("amount_due") BigDecimal amountDue,
        String currency,
        String status
) {}
package com.yourcompany.payments.dto.nyaradzo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record NyaradzoValidationResponse(
        @JsonProperty("policy_number") String policyNumber,
        @JsonProperty("policy_holder") String policyHolder,
        @JsonProperty("months_validated") int monthsValidated,
        @JsonProperty("policy_status") String policyStatus,
        @JsonProperty("monthly_premium") BigDecimal monthlyPremium,
        @JsonProperty("amount_due") BigDecimal amountDue,
        String currency,
        boolean successful,
        String message
) {}
package com.yourcompany.payments.dto.nyaradzo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yourcompany.payments.dto.nyaradzo.PolicyStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class NyaradzoValidationResponse {
    @JsonProperty("policy_number")
    private String policyNumber;

    @JsonProperty("policy_holder")
    private String policyHolder;

    @JsonProperty("months_validated")
    private int monthsValidated;

    @JsonProperty("policy_status")
    private PolicyStatus policyStatus;

    @JsonProperty("monthly_premium")
    private BigDecimal monthlyPremium;

    @JsonProperty("amount_due")
    private BigDecimal amountDue;

    private String currency;
    private boolean successful;
    private String message;
}